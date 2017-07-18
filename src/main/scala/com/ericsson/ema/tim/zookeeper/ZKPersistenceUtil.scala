package com.ericsson.ema.tim.zookeeper

import java.net.InetAddress

import com.ericsson.util.SystemPropertyUtil
import org.apache.zookeeper.KeeperException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/6/23.
  */
object ZKPersistenceUtil {
	private[this] val LOGGER = LoggerFactory.getLogger(ZKPersistenceUtil.getClass)
	private[this] val zkRootPath = Try(SystemPropertyUtil.getAndAssertProperty("com.ericsson.ema.tim.zkRootPath")).getOrElse("/TIM_POC")

	private[this] val zkm = ZKConnectionManager()

	//get zookeeper connection, throw exception if connection is null in ZKConnectionManager
	private[this] def getConnection = zkm.getConnection.getOrElse(throw new KeeperException.ConnectionLossException)

	private[this] def isConnected = getConnection.getState.isConnected

	private[this] def exists(path: String): Boolean = Try(getConnection.exists(path, false) != null).getOrElse(false)

	var addOn: String = getIdentifier

	private[this] def setNodeData(path: String, data: String) = synchronized {
		if (isConnected)
			Try(getConnection.setData(path, data.getBytes, -1)) match {
				case Success(_)  =>
				case Failure(ex) => LOGGER.error("Failed to setNodeData: " + ex.getMessage)
					throw new RuntimeException(ex)
			}
	}

	private def getNodeData(path: String) = {
		if (isConnected)
			Try {
				//we don't need to register watcher for this get, since it is for debug purpose
				val byteData = getConnection.getData(path, false, null)
				new String(byteData, "utf-8")
			} match {
				case Success(result) => result
				case Failure(ex)     => LOGGER.error("Failed to getNodeData: " + ex.getMessage); ""
			}
		else ""
	}

	def persist(table: String, data: String): Unit = {
		val tabPath = zkRootPath + "/" + table
		if (!exists(tabPath))
			throw new RuntimeException("root path " + zkRootPath + " does not exist!")
		else {
			LOGGER.debug("set znode {}" + tabPath)
			setNodeData(tabPath, data)
		}
		if (LOGGER.isDebugEnabled)
			LOGGER.debug("{} data: {}", tabPath, getNodeData(tabPath): Any)
	}

	//getIdentifier is the first line of the zonde.
	//it records the pid and host who has this zkcache.
	private[this] def getIdentifier(): String = {
		import java.lang.management.ManagementFactory
		val pid: String = ManagementFactory.getRuntimeMXBean.getName
		val addr: InetAddress = InetAddress.getLocalHost;
		val result = pid + "@" + addr.getHostName + "endHead"
		result
	}
}
