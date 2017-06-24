package com.ericsson.ema.tim.zookeeper

import java.util.concurrent.locks.ReentrantLock

import com.ericsson.ema.tim.context.{MetaDataRegistry, Tab2ClzMap, Tab2MethodInvocationCacheMap, TableInfoMap}
import com.ericsson.ema.tim.json.JsonLoader
import com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock
import com.ericsson.ema.tim.pojo.{NameType, PojoGenerator, Table, TableTuple}
import com.ericsson.ema.tim.reflection.TabDataLoader
import com.ericsson.ema.tim.zookeeper.State.State
import com.ericsson.util.SystemPropertyUtil
import com.ericsson.zookeeper.{NodeChildCache, NodeChildrenChangedListener, ZooKeeperUtil}
import org.apache.zookeeper.CreateMode.PERSISTENT
import org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE
import org.apache.zookeeper.{KeeperException, WatchedEvent, Watcher, ZooKeeper}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/5/5.
  */
class ZKMonitor(zkConnectionManager: ZKConnectionManager) {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[ZKMonitor])

	private[this] var zkRootPath: String = _
	private[this] var nodeChildCache: NodeChildCache = _
	private[this] val lock = new ReentrantLock

	def start(): Unit = {
		zkRootPath = Try(SystemPropertyUtil.getAndAssertProperty("com.ericsson.ema.tim.zkRootPath")).getOrElse("/TIM_POC")
		zkConnectionManager.registerListener(new ZooKeeperConnectionStateListenerImpl)
		Try(ZooKeeperUtil.createRecursive(getConnection, zkRootPath, null, OPEN_ACL_UNSAFE, PERSISTENT)) match {
			case Success(_)  =>
			case Failure(ex) => LOGGER.error("Failed to start ZKMonitor, the exception is ", ex.getMessage)
				throw new RuntimeException(ex)
		}
		loadAllTable()
	}

	def stop(): Unit = {
		Option(nodeChildCache).foreach(_.stop)
		unloadAllTable()
	}

	private[this] def loadAllTable(): Unit = {
		unloadAllTable()
		val children: List[String] = Try {
			nodeChildCache = new NodeChildCache(getConnection, zkRootPath, new NodeChildrenChangedListenerImpl)
			import scala.collection.JavaConversions._
			nodeChildCache.start.toList
		} match {
			case Success(result) => result
			case Failure(ex)     =>
				LOGGER.warn("Failed to setup nodeChildCache due to ", ex.getMessage)
				throw new RuntimeException(ex)
		}
		childrenAdded(children)
	}

	//thread safe
	private[this] def loadOneTable(zkNodeName: String): Unit = {
		LOGGER.debug("Start to load data for node {}", zkNodeName)
		lock.lock()
		try {
			val rawData = zkConnectionManager.getConnection.map(getDataZKNoException(_, zkRootPath + "/" + zkNodeName, new NodeWatcher(zkNodeName)))
				.getOrElse(new Array[Byte](0))
			if (rawData.isEmpty) {
				LOGGER.error("Failed to loadOneTable for node {}", zkNodeName)
				return
			}
			doLoad(zkNodeName, new String(rawData))
		} finally {
			lock.unlock()
		}
	}

	def doLoad(tableName: String, content: String): Unit = {
		//1. load json
		val jloader = loadJsonFromRawData(content, tableName)
		var needToInvalidateInvocationCache = false
		if (!isMetaDataDefined(jloader)) { //metadata change-> function need re-reflection
			Tab2ClzMap().unRegister(tableName)
			needToInvalidateInvocationCache = true
			//2. parse json cache and build as datamodel
			val table = buildDataModelFromJson(jloader)
			//3. generate pojo class
			PojoGenerator.generateTableClz(table)
			updateMetaData(jloader)
		}
		//4. load data by reflection, and the new data will replace old one.
		val obj = loadDataByReflection(jloader)

		//8. registerOrReplace tab into global registry
		LOGGER.info("=====================registerOrReplace {}=====================", tableName)
		//force original loaded obj and its classloader to gc
		zkCacheRWLock.writeLockTable(tableName)
		try {
			if (needToInvalidateInvocationCache)
				Tab2MethodInvocationCacheMap().unRegister(tableName)
			TableInfoMap().registerOrReplace(tableName, jloader.tableMetadata, obj)
		} finally {
			zkCacheRWLock.writeUnLockTable(tableName)
		}
	}

	private[this] def loadJsonFromRawData(json: String, tableName: String) = {
		val jloader = new JsonLoader(tableName)
		jloader.loadJsonFromString(json)
		jloader
	}

	private[this] def buildDataModelFromJson(jloader: JsonLoader): Table = {
		LOGGER.info("=====================parse json=====================")
		val tt = new TableTuple("records", jloader.tableName + "Data")
		tt.tuples = jloader.tableMetadata.foldRight(List[NameType]())((kv, list) => NameType(kv._1, kv._2) :: list)
		val table = Table(jloader.tableName, tt)
		LOGGER.debug("Table structure: {}", table)
		table
	}

	private[this] def loadDataByReflection(jloader: JsonLoader): Object = {
		LOGGER.info("=====================load data by reflection=====================")
		val classToLoad = PojoGenerator.pojoPkg + "." + jloader.tableName
		Try(TabDataLoader(classToLoad, jloader).loadData()) match {
			case Success(result) => result
			case Failure(ex)     => LOGGER.warn("Failed to loadDataByReflection: " + ex.getMessage)
				throw new RuntimeException(ex.getMessage)
		}
	}

	private[this] def isMetaDataDefined(jsonLoader: JsonLoader): Boolean = {
		val defined = MetaDataRegistry().isRegistered(jsonLoader.tableName, jsonLoader.tableMetadata.toMap)
		if (defined) LOGGER.info("Metadata already defined for {}, skip regenerating javabean...", jsonLoader.tableName)
		else LOGGER.info("Metadata NOT defined for {}", jsonLoader.tableName)
		defined
	}

	private[this] def updateMetaData(jsonLoader: JsonLoader) =
		MetaDataRegistry().registerMetaData(jsonLoader.tableName, jsonLoader.tableMetadata.toMap)

	private[this] def getDataZKNoException(zooKeeper: ZooKeeper, zkTarget: String, watcher: Watcher) =
		Try(zooKeeper.getData(zkTarget, watcher, null)).getOrElse(new Array[Byte](0))

	private[this] def getConnection =
		zkConnectionManager.getConnection.getOrElse(throw new KeeperException.ConnectionLossException)

	private[this] def unloadAllTable() = {
		LOGGER.info("=====================unregister all table=====================")
		MetaDataRegistry().clear()
		TableInfoMap().clear()
		Tab2MethodInvocationCacheMap().clear()
		Tab2ClzMap().clear()
	}

	private[this] def unloadOneTable(zkNodeName: String) = {
		LOGGER.info("=====================registerOrReplace {}=====================", zkNodeName)
		MetaDataRegistry().unregisterMetaData(zkNodeName)
		TableInfoMap().unregister(zkNodeName)
		Tab2MethodInvocationCacheMap().unRegister(zkNodeName)
		Tab2ClzMap().unRegister(zkNodeName)
	}

	private[this] def childrenAdded(children: List[String]): Unit = {
		children.foreach(loadOneTable)
	}

	private[this] def childrenRemoved(children: List[String]): Unit = {
		children.foreach(unloadOneTable)
	}

	private[this] class NodeChildrenChangedListenerImpl extends NodeChildrenChangedListener {

		import scala.collection.JavaConversions._

		override def childAdded(children: java.util.List[String]): Unit = {
			childrenAdded(children.toList)
		}

		override def childRemoved(children: java.util.List[String]): Unit = {
			childrenRemoved(children.toList)
		}

		override def terminallyFailed(): Unit = {
			LOGGER.error("Unexpected failure happens!")
		}
	}

	private[this] class ZooKeeperConnectionStateListenerImpl extends ZKConnectionChangeWatcher {
		override def stateChange(state: State): Unit = {
			state match {
				case State.CONNECTED | State.RECONNECTED => loadAllTable()
				case State.DISCONNECTED                  => Option(nodeChildCache).foreach(_.stop)
			}
		}
	}

	private[this] class NodeWatcher(val zkNodeName: String) extends Watcher {
		override def process(event: WatchedEvent): Unit = {
			if (event.getType == Watcher.Event.EventType.NodeDataChanged)
				loadOneTable(zkNodeName)
		}
	}

}


