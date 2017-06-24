package com.ericsson.ema.tim.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
  * Created by eqinson on 2017/5/5.
  */
object ZKCacheRWLockMap {
	private[this] val instance = new ZKCacheRWLockMap

	def zkCacheRWLock: ZKCacheRWLockMap = instance
}

class ZKCacheRWLockMap private() {
	private[this] val map = new ConcurrentHashMap[String, ZKCacheRWLock]

	def readLockTable(table: String): Unit = {
		map.computeIfAbsent(table, (k: String) => new ZKCacheRWLock).readLock()
	}

	def readUnLockTable(table: String): Unit = {
		if (!map.containsKey(table))
			throw new IllegalStateException("Not gain table read lock yet before read unlock")
		map.get(table).readUnlock()
	}

	def writeLockTable(table: String): Unit = {
		map.computeIfAbsent(table, (k: String) => new ZKCacheRWLock).writeLock()
	}

	def writeUnLockTable(table: String): Unit = {
		if (!map.containsKey(table))
			throw new IllegalStateException("Not gain table write lock yet before write unlock")
		map.get(table).writeUnlock()
	}

	import java.util.function.{Function => JavaFunction}

	implicit def scalaFunctionToJava[From, To](scalafunction: (From) => To): JavaFunction[From, To] = {
		new JavaFunction[From, To] {
			override def apply(input: From): To = scalafunction(input)
		}
	}

	private[this] class ZKCacheRWLock {
		private[this] val rwl = new ReentrantReadWriteLock

		def readLock(): Unit = {
			rwl.readLock.lock()
		}

		def readUnlock(): Unit = {
			rwl.readLock.unlock()
		}

		def writeLock(): Unit = {
			rwl.writeLock.lock()
		}

		def writeUnlock(): Unit = {
			rwl.writeLock.unlock()
		}
	}

}

