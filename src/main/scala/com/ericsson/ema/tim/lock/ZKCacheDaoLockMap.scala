package com.ericsson.ema.tim.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
  * Created by eqinson on 2017/5/26.
  */
object ZKCacheDaoLockMap {
	private[this] val instance = new ZKCacheDaoLockMap

	def zkCacheDaoLock: ZKCacheDaoLockMap = instance
}

class ZKCacheDaoLockMap private() {
	private[this] val map = new ConcurrentHashMap[String, ReentrantLock]

	def lockTable(table: String): Unit = {
		map.computeIfAbsent(table, (k: String) => new ReentrantLock).lock()
	}

	def unLockTable(table: String): Unit = {
		if (!map.containsKey(table))
			throw new IllegalStateException("Not gain table read lock yet before read unlock")
		map.get(table).unlock()
	}

	import java.util.function.{Function => JavaFunction}

	implicit def scalaFunctionToJava[From, To](scalafunction: (From) => To): JavaFunction[From, To] = {
		new JavaFunction[From, To] {
			override def apply(input: From): To = scalafunction(input)
		}
	}
}
