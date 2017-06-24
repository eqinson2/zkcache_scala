package com.ericsson.ema.tim.json

import org.json.JSONObject
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by eqinson on 2017/5/5.
  */
case class FieldInfo(fieldValue: String, fieldName: String, fieldType: String) {
	override def toString: String = "FieldInfo{" + "fieldValue='" + fieldValue + '\'' + ", fieldName='" + fieldName + '\'' + ", fieldType='" + fieldType + '\'' + '}'
}

case class TypeInfo(theName: String, theType: String) {
	override def toString: String = "TypeInfo{" + "name='" + theName + '\'' + ", type='" + theType + '\'' + '}'
}


class JsonLoader(var tableName: String) {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[JsonLoader])

	private[this] val TABLE_TAG = "Table"
	private[this] val ID_TAG = "Id"
	private[this] val TABLE_HEADER_TAG = "Header"
	private[this] val TABLE_CONTENT_TAG = "Content"
	private[this] val TABLE_TUPLE_TAG = "Tuple"
	private[this] val PATTERN = "\\{[\\w ]+\\}".r
	private[this] var tableHeaderIndexMap: Map[Integer, TypeInfo] = Map[Integer, TypeInfo]()

	val tableMetadata: mutable.Map[String, String] = mutable.LinkedHashMap[String, String]()
	var tupleList: List[List[FieldInfo]] = List[List[FieldInfo]]()

	private[this] def trimBrace(s: String): String = {
		if (s.length >= 2) s.substring(1, s.length - 1)
		else ""
	}

	private[this] def parseTableHeader(root: JSONObject): Unit = {
		val arr = root.getJSONArray(TABLE_HEADER_TAG)
		for (i <- 0 until arr.length) {
			val keys = arr.getJSONObject(i).keys
			while (keys.hasNext) {
				val key = keys.next
				arr.getJSONObject(i).get(key) match {
					case t: String =>
						tableHeaderIndexMap += (Integer.valueOf(i) -> TypeInfo(key, t))
						tableMetadata.put(key, t)
					case _         => throw new ClassCastException("bug: illegal type...")
				}
			}
		}
	}

	private[this] def parseTableContent(root: JSONObject): Unit = {
		val arr = root.getJSONArray(TABLE_CONTENT_TAG)
		for (i <- 0 until arr.length) {
			val content = arr.getJSONObject(i).getString(TABLE_TUPLE_TAG)
			val tuple = for ((matchedField, column) <- PATTERN.findAllIn(content).zipWithIndex.toList;
							 f = tableHeaderIndexMap(column))
				yield FieldInfo(trimBrace(matchedField), f.theName, f.theType)
			tupleList :+= tuple
		}
	}

	def loadJsonFromString(jsonStr: String): Unit = {
		val obj = new JSONObject(jsonStr)
		val table = obj.getJSONObject(TABLE_TAG)
		if (Option(tableName).isEmpty)
			tableName = table.getString(ID_TAG)

		parseTableHeader(table)
		if (LOGGER.isDebugEnabled) {
			tableHeaderIndexMap.foreach(kv => LOGGER.debug("key : {}, value: {}", kv._1, kv._2: Any))
			tableMetadata.foreach(kv => LOGGER.debug("key : {}, value: {}", kv._1, kv._2: Any))
		}

		parseTableContent(table)
		if (LOGGER.isDebugEnabled) {
			tupleList.foreach(_.foreach(LOGGER.debug("field info: {}", _)))
		}
	}
}