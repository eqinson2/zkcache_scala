package com.ericsson.ema.tim.zookeeper

import com.ericsson.util.SystemPropertyUtil
import org.apache.zookeeper.KeeperException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/6/23.
  */
object ZKPersistenceUtil {
	private[this] val LOGGER = LoggerFactory.getLogger(ZKPersistenceUtil.getClass)
	private[this] val zkRootPath = SystemPropertyUtil.getAndAssertProperty("com.ericsson.ema.tim.zkRootPath")

	private[this] val zkm = ZKConnectionManager()

	private[this] def getConnection = zkm.getConnection.getOrElse(throw new KeeperException.ConnectionLossException)

	private[this] def isConnected = getConnection.getState.isConnected

	private[this] def exists(path: String): Boolean = Try(getConnection.exists(path, false) != null).getOrElse(false)

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
				val byteData = getConnection.getData(path, true, null)
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
			throw new RuntimeException("root path " + zkRootPath + "does not exist!")
		else {
			LOGGER.debug("set znode {}" + tabPath)
			setNodeData(tabPath, data)
		}
		if (LOGGER.isDebugEnabled)
			LOGGER.debug("{} data: {}", tabPath, getNodeData(tabPath): Any)
	}
}
