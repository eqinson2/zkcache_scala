package com.ericsson.ema.tim.context

import com.ericsson.ema.tim.reflection.MethodInvocationCache

/**
  * Created by eqinson on 2017/5/5.
  */
class Tab2MethodInvocationCacheMap {
	private[this] var map = Map[String, MethodInvocationCache]()

	def clear(): Unit = {
		map = Map[String, MethodInvocationCache]()
	}

	def unRegister(tableName: String): Unit = {
		map.get(tableName).foreach(_.cleanup())
		map -= tableName
	}

	def lookup(tableName: String): MethodInvocationCache = {
		map.get(tableName) match {
			case Some(cache) => cache
			case None        =>
				val cache = new MethodInvocationCache()
				map += (tableName -> cache)
				cache
		}
	}

}

object Tab2MethodInvocationCacheMap {
	private[this] val instance = new Tab2MethodInvocationCacheMap

	def apply(): Tab2MethodInvocationCacheMap = instance
}
