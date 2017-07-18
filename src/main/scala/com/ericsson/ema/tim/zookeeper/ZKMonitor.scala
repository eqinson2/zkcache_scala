package com.ericsson.ema.tim.zookeeper

import java.util.concurrent.locks.ReentrantLock

import com.ericsson.ema.tim.context.{MetaDataRegistry, Tab2ClzMap, Tab2MethodInvocationCacheMap, TableInfoMap}
import com.ericsson.ema.tim.json.JsonLoader
import com.ericsson.ema.tim.lock.DirtyMap
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
    //create znode /TIM_POC is not exist
    //ZooKeeperUtil is a ericsson API for zookeeper
    Try(ZooKeeperUtil.createRecursive(getConnection, zkRootPath, null, OPEN_ACL_UNSAFE, PERSISTENT)) match {
      case Success(_) => LOGGER.info(s"The zkpath $zkRootPath is created successfully.")
      case Failure(ex) => LOGGER.warn(s"The zkpath $zkRootPath already exists: ", ex.getMessage)
    }
    LOGGER.debug("init loadAllTable is triggered")
    //loadAllTable only triggered once during starting,
    // because ZooKeeperConnectionStateListenerImpl is registered after first Event.KeeperState.SyncConnected event
    loadAllTable()
  }

  def stop(): Unit = {
    Option(nodeChildCache).foreach(_.stop)
    unloadAllTable()
  }

  private[this] def loadAllTable(): Unit = {
    LOGGER.debug("now begin to loadAllTables")
    unloadAllTable()
    val children: List[String] = Try {
      //NodeChildrenChangedListenerImpl is triggered when a child node is added or deleted
      //NodeChildCache is a ericsson API for zookeeper, it will automatically register new watcher every time. you only need to care NodeChildrenChangedListenerImpl here
      nodeChildCache = new NodeChildCache(getConnection, zkRootPath, new NodeChildrenChangedListenerImpl)
      import scala.collection.JavaConversions._
      //get all child nodes of /TIM_POC
      //return a JAVA Collections.unmodifiableList(this.knownChildren);
      nodeChildCache.start.toList
    } match {
      case Success(result) => result
      case Failure(ex) =>
        LOGGER.warn("Failed to setup nodeChildCache due to ", ex.getMessage)
        throw new RuntimeException(ex)
    }
    childrenAdded(children)
  }

  //thread safe
  /**
    * loadOneTable is triggered when reconnect or connected happened, or znode is changed by other JVM
    *
    * registerWatcher: we use original zookeeper watcher for data change. only need to register watch when first time we load a table.
    *                  next time when data change, NodeWatcher will handle the event and register a new watcher.
    *
    */
  private[this] def loadOneTable(zkNodeName: String, registerWatcher: Boolean = true): Unit = {
    LOGGER.debug("Start loadOneTable to load data for node {}", zkNodeName)
    lock.lock()
    try {
      //if the node data is changed, NodeWatcher will trigger a reload of this table
      var wt: NodeWatcher = null
      if (registerWatcher)
        wt = new NodeWatcher(zkNodeName)
      val rawData = zkConnectionManager.getConnection.map(getDataZKNoException(_, zkRootPath + "/" + zkNodeName, wt))
        .getOrElse(new Array[Byte](0))
      if (rawData.isEmpty) {
        LOGGER.error("Failed to loadOneTable for node {}", zkNodeName)
        return
      }
      doLoad(zkNodeName, stripIdentifier(new String(rawData)))

      //every time when we load a table, we need to tell JDV API by set a flag in DirtyMap
      DirtyMap.setDirty(zkNodeName)
    } finally {
      lock.unlock()
    }
  }

  /**
    * the head information is the first line of znode. it is like pid+@+hostname+"endHead"
    * when zkcache load znode from zookeeper, it will strip this line.
    *

    */
  private[this] def stripIdentifier(input: String): String = {
    val result = input.replaceFirst("""(\S+\n)""", "")
    LOGGER.debug("result after strip header {}", result)
    result
  }

  private[this] def stripAndGetIdentifier(input: String): String = {
    val p = """(\S+endHead)""".r
    val head = p.findFirstMatchIn(input).getOrElse("").toString
    head
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
    // new obj is the instance of <tablename>.class
    val obj = loadDataByReflection(jloader)

    //8. registerOrReplace tab into global registry
    LOGGER.info("=====================registerOrReplace {}=====================", tableName)
    //force original loaded obj and its classloader to gc
    zkCacheRWLock.writeLockTable(tableName)
    try {
      if (needToInvalidateInvocationCache)
        Tab2MethodInvocationCacheMap().unRegister(tableName)
      //obj is the class instance contains records
      TableInfoMap().registerOrReplace(tableName, jloader.tableMetadata, obj)
    } finally {
      zkCacheRWLock.writeUnLockTable(tableName)
    }
  }

  //tableMetadata: mutable.Map[String, String]
  //tupleList: List[List[FieldInfo]], FieldInfo("SH", "hometown","String")
  //List[FieldInfo].size == column.size
  private[this] def loadJsonFromRawData(json: String, tableName: String) = {
    val jloader = new JsonLoader(tableName)
    jloader.loadJsonFromString(json)
    jloader
  }

  private[this] def buildDataModelFromJson(jloader: JsonLoader): Table = {
    LOGGER.info("=====================parse json=====================")
    val tt = new TableTuple("records", jloader.tableName + "Data")
    //List[NameType]() is the start value
    //tuples is a List[NameType]  NameType("name", "String")
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
      case Failure(ex) => LOGGER.warn("Failed to loadDataByReflection: " + ex.getMessage)
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

  //unload all table is clear all cache in
  //MetaDataRegistry  TableInfoMap Tab2MethodInvocationCacheMap and Tab2ClzMap
  private[this] def unloadAllTable() = {
    LOGGER.info("=====================unregister all table=====================")
    MetaDataRegistry().clear()
    TableInfoMap().clear()
    Tab2MethodInvocationCacheMap().clear()
    Tab2ClzMap().clear()
  }

  //unload table is clear all cache in
  //MetaDataRegistry  TableInfoMap Tab2MethodInvocationCacheMap and Tab2ClzMap
  private[this] def unloadOneTable(zkNodeName: String) = {
    LOGGER.info("=====================registerOrReplace {}=====================", zkNodeName)
    MetaDataRegistry().unregisterMetaData(zkNodeName)
    TableInfoMap().unregister(zkNodeName)
    Tab2MethodInvocationCacheMap().unRegister(zkNodeName)
    Tab2ClzMap().unRegister(zkNodeName)
  }

  private[this] def childrenAdded(children: List[String]): Unit = {
    //the list children contains all the children nodes,
    // by default, we need to register watcher when load table
    children.foreach(x => loadOneTable(x))
  }

  private[this] def childrenRemoved(children: List[String]): Unit = {
    //the list children contains all the children nodes removed
    children.foreach(unloadOneTable)
  }


/*
* NodeChildrenChangedListenerImpl is triggered when a child node is added or deleted
* NodeChildCache is a ericsson API for zookeeper, it will automatically register new watcher every time. you only need to care NodeChildrenChangedListenerImpl here
* */
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

  /*
* ZooKeeperConnectionStateListenerImpl is triggered when default watcher ConnectionWatcher is triggered.
*
* */
  private[this] class ZooKeeperConnectionStateListenerImpl extends ZKConnectionChangeWatcher {
    override def stateChange(state: State): Unit = {
      LOGGER.debug("ZooKeeperConnectionStateListenerImpl is called" + state.toString)
      state match {
        case State.CONNECTED | State.RECONNECTED =>
          LOGGER.debug("ZooKeeperConnectionStateListenerImpl CONNECTED/RECONNECTED is triggered" + state.toString)
          loadAllTable()
        case State.DISCONNECTED => Option(nodeChildCache).foreach(_.stop)
      }
    }
  }

  /*
* NodeWatcher is triggered when the data of znode is changed.
*
*
* */
  private[this] class NodeWatcher(val zkNodeName: String) extends Watcher {
    override def process(event: WatchedEvent): Unit = {
      if (event.getType == Watcher.Event.EventType.NodeDataChanged) {
        println("in NodeWatcher")
        //every time when we get data change, we need to tell JDV API by set a flag in DirtyMap
        DirtyMap.setDirty(zkNodeName)
        //get data and register a new watcher, zookeeper watcher is one time trigger, so do remember to register new watcher.
        val rawData = zkConnectionManager.getConnection.map(getDataZKNoException(_, zkRootPath + "/" + zkNodeName, new NodeWatcher(zkNodeName)))
          .getOrElse(new Array[Byte](0))
        val h = stripAndGetIdentifier(new String(rawData))
        LOGGER.debug("stripAndGetIdentifier in NodeWatcher header {}", h.toString)
        LOGGER.debug("getIdentifier in NodeWatcher head here is:" + ZKPersistenceUtil.addOn.toString)
        val debug = ZKPersistenceUtil.addOn == h.toString
        LOGGER.debug("NodeWatcher result:" + debug)

        //if the head in data is the same with ZKPersistenceUtil.addOn, it means it is this process who trigger the change and we skip a node to get better performance
        //if not, it mean the change is done by other process, we need to re-load this table to fresh cache.
        if (ZKPersistenceUtil.addOn.toString != h.toString) {
          loadOneTable(zkNodeName, false)
        }
      }
    }
  }

}


