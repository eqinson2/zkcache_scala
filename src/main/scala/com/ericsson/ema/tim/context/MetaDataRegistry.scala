package com.ericsson.ema.tim.context

/**
  * Created by eqinson on 2017/5/5.
	* record table meta data.
	* Map[String, Map[String, String]] : jsonLoader.tableName, jsonLoader.tableMetadata.toMap
	* "Eqinson", tableMetadata: mutable.Map[String, String]
  */
class MetaDataRegistry {
	private[this] var registry = Map[String, Map[String, String]]()

	def registerMetaData(tableName: String, metadata: Map[String, String]): Unit = {
		registry += (tableName -> metadata)
	}

	def unregisterMetaData(tableName: String): Unit = {
		registry -= tableName
	}

	def clear(): Unit = {
		registry = Map[String, Map[String, String]]()
	}

	def isRegistered(tableName: String, other: Map[String, String]): Boolean = {
		registry.exists(_._1 == tableName) && registry(tableName) == other
	}

}

object MetaDataRegistry {
	private[this] val instance = new MetaDataRegistry

	def apply(): MetaDataRegistry = instance
}
