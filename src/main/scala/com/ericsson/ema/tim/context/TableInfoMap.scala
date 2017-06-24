package com.ericsson.ema.tim.context

import scala.collection.mutable

/**
  * Created by eqinson on 2017/5/5.
  */
class TableInfoMap {
	private[this] var registry = Map[String, TableInfoContext]()

	def clear(): Unit = {
		registry = Map[String, TableInfoContext]()
	}

	def unregister(tableName: String): Unit = {
		registry -= tableName
	}

	def lookup(tableName: String): Option[TableInfoContext] = registry.get(tableName)

	def lookupAll(): Map[String, TableInfoContext] = registry

	def registerOrReplace(tablename: String, tableMetadata: mutable.Map[String, String], tableData: Object): Unit = {
		registry += (tablename -> TableInfoContext(tableData, tableMetadata))
	}
}

object TableInfoMap {
	private[this] val instance = new TableInfoMap

	def apply(): TableInfoMap = instance
}

case class TableInfoContext(tabledata: Object, tableMetadata: mutable.Map[String, String])
