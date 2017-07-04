package com.ericsson.ema.tim.zookeeper

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import com.ericsson.ema.tim.zookeeper.State.State
import com.ericsson.util.SystemPropertyUtil
import com.ericsson.zookeeper.ZooKeeperUtil
import org.apache.zookeeper.Watcher.Event
import org.apache.zookeeper.{WatchedEvent, Watcher, ZooKeeper}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/5/5.
  */
trait ZKConnectionChangeWatcher {
	def stateChange(state: State): Unit
}


class ZKConnectionManager {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[ZKConnectionManager])

	private[this] val SESSION_TIMEOUT = 6000
	private[this] var connectStr: String = _
	private[this] var zooKeeper: ZooKeeper = _
	private[this] var listeners = List[ZKConnectionChangeWatcher]()

	@volatile
	private[this] var waitForReconnect: Boolean = _
	private[this] var reconnectExecutor: ExecutorService = _
	private[this] var reconnFuture: Future[_] = _

	connectStr = Try(SystemPropertyUtil.getAndAssertProperty("com.ericsson.ema.tim.zkconnstr")).getOrElse("localhost:6181")

	def init(): Unit = {
		LOGGER.info("Start to init zookeeper connection manager.")
		while (!connect()) {
			try {
				Thread.sleep(30 * 1000)
			} catch {
				case e: InterruptedException => LOGGER.debug("connect interrupted from sleep, try again...")
			}
		}
		reconnectExecutor = Executors.newSingleThreadExecutor(new ZKNamedSequenceThreadFactory("ZKReconnect"))
		reconnFuture = reconnectExecutor.submit(new Runnable {
			override def run(): Unit = {
				while (!Thread.currentThread.isInterrupted) {
					try {
						Thread.sleep(60 * 1000)
						if (waitForReconnect && getConnection.isEmpty) {
							LOGGER.info("ZK reconnection is wanted.")
							connect()
						}
					} catch {
						case e: InterruptedException =>
							LOGGER.info("Interrupted from sleep, return directly.")
							return
						case e: Exception            =>
							LOGGER.error("Unexpected error happens", e)
					}

				}
			}
		})
	}

	private[this] def connect(): Boolean = {
		Try {
			val watcher = new ConnectionWatcher
			zooKeeper = new ZooKeeper(connectStr, SESSION_TIMEOUT, watcher)
			watcher.waitUntilConnected()
		} match {
			case Success(_)  => true
			case Failure(ex) => LOGGER.warn("Failed to create zookeeper connection: " + ex.getMessage)
				zooKeeper = null
				false
		}
	}

	def destroy(): Unit = {
		LOGGER.info("Start to destroy zookeeper connection manager.")
		reconnFuture.cancel(false)
		reconnectExecutor.shutdownNow
		try {
			if (!reconnectExecutor.awaitTermination(60, TimeUnit.SECONDS))
				LOGGER.warn("Failed to shutdown the reconnect monitor immediately.")
		}
		catch {
			case e: InterruptedException => LOGGER.warn("interrupted from await for termination")
		}
		getConnection.foreach(ZooKeeperUtil.closeNoException)
		zooKeeper = null
	}

	def getConnection: Option[ZooKeeper] = Option(zooKeeper)

	def registerListener(listener: ZKConnectionChangeWatcher): Unit = {
		listeners :+= listener
	}

	private[this] def notifyListener(state: State): Unit = {
		listeners.foreach(_.stateChange(state))
	}

	private[this] def getSessionId: String = getConnection.map(z => "0x" + java.lang.Long.toHexString(z.getSessionId)).getOrElse("NO-SESSION")

	private[this] class ConnectionWatcher extends Watcher {
		private[this] val latch = new CountDownLatch(1)
		private[this] var connectionHasBeenEstablished = false

		override def process(event: WatchedEvent): Unit = {
			event.getState match {
				case Event.KeeperState.Expired       =>
					LOGGER.error("The session [{}] in ZK has been expired will perform an automatic re-connection " + "attempt", getSessionId)
					connect()
					if (getConnection.isEmpty && !waitForReconnect) {
						LOGGER.error("Failed to reconnect the zookeeper server")
						waitForReconnect = true
					}
				case Event.KeeperState.SyncConnected =>
					LOGGER.info("Got connected event for session [{}] to zookeeper", getSessionId)
					if (connectionHasBeenEstablished)
						notifyListener(State.RECONNECTED)
					else
						notifyListener(State.CONNECTED)
					connectionHasBeenEstablished = true
					waitForReconnect = false
					latch.countDown()
				case Event.KeeperState.Disconnected  =>
					LOGGER.warn("The session [{}] in ZooKeeper has lost its connection", getSessionId)
					notifyListener(State.DISCONNECTED)
				case _                               => LOGGER.error("should not happen")
			}
		}

		def waitUntilConnected(): Unit = {
			try
				latch.await(60, TimeUnit.SECONDS)
			catch {
				case e: InterruptedException => LOGGER.trace(e.getMessage)
			}
		}
	}

	private[this] class ZKNamedSequenceThreadFactory(val threadName: String) extends ThreadFactory {
		private[this] val counter = new AtomicLong(1L)

		override def newThread(runnable: Runnable) = new Thread(runnable, this.threadName + "-" + this.counter.getAndIncrement)
	}

}

object ZKConnectionManager {
	private[this] var instance: ZKConnectionManager = _

	def apply(): ZKConnectionManager = synchronized {
		Option(instance) match {
			case None    =>
				instance = new ZKConnectionManager
				instance
			case Some(_) => instance
		}
	}
}

object State extends Enumeration {
	private[zookeeper] type State = Value
	val CONNECTED, RECONNECTED, DISCONNECTED = Value
}





