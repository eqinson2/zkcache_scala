package com.ericsson.ema.tim.dml

import java.lang.reflect.Method
import java.net.InetAddress

import com.ericsson.ema.tim.context.{Tab2MethodInvocationCacheMap, TableInfoContext, TableInfoMap}
import com.ericsson.ema.tim.exception.{DmlBadSyntaxException, DmlNoSuchFieldException}
import com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock
import com.ericsson.ema.tim.reflection.{AccessType, MethodInvocationCache}
import com.ericsson.ema.tim.zookeeper.ZKPersistenceUtil
import org.json.{JSONArray, JSONObject}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/6/23.
  */
abstract class Operator {
	protected val TUPLE_FIELD = "records"
	protected var table: String = _
	protected var records: List[Object] = _

	var context: TableInfoContext = _
	var methodInvocationCache: MethodInvocationCache = _

	protected def initExecuteContext(): Unit = {
		//context is TableInfoContext(tableData, tableMetadata)
		this.context = TableInfoMap().lookup(table).getOrElse(throw DmlBadSyntaxException("Error: Selecting a " + "non-existing table:" + table))
		this.methodInvocationCache = Tab2MethodInvocationCacheMap().lookup(table)
		//it is safe because records must be List according to JavaBean definition
		val tupleField = invokeGetByReflection(context.tabledata, TUPLE_FIELD)
		import scala.collection.JavaConversions._
		this.records = tupleField.asInstanceOf[java.util.List[Object]].toList
	}

	protected def invokeGetByReflection(obj: Object, wantedField: String): Object = {
		val getter = methodInvocationCache.get(obj.getClass, wantedField, AccessType.GET)
		Try(getter.invoke(obj)).getOrElse(throw DmlBadSyntaxException("invokeGetByReflection error!"))
	}

	protected def invokeSetByReflection(obj: Object, wantedField: String, newValue: Object): Unit = {
		val setter = methodInvocationCache.get(obj.getClass, wantedField, AccessType.SET)
		Try(setter.invoke(obj, newValue)) match {
			case Success(_)  =>
			case Failure(ex) => throw DmlBadSyntaxException("invokeSetByReflection error: " + ex.getMessage)
		}
	}
}

abstract class ChangeOperator extends Operator {
	private val LOGGER = LoggerFactory.getLogger(classOf[ChangeOperator])

	protected def cloneList(original: List[Object]): List[Object] = {
		if (original.isEmpty)
			return List[Object]()

		def clone(): List[Object] = {

			val cloneMethod: Method = original.head.getClass.getDeclaredMethod("clone")
			cloneMethod.setAccessible(true)
			original.map(cloneMethod invoke _)
		}

		Try(clone()).getOrElse(throw new RuntimeException("cloneList error!"))
	}

	private[this] def toJson(listOfObj: List[Object]): JSONObject = {
		val json = new JSONObject
		val tableBody = new JSONObject
		json.put("Table", tableBody)
		tableBody.put("Id", this.table)
		val headerArray = new JSONArray
		tableBody.put("Header", headerArray)

		this.context.tableMetadata.foreach(kv => {
			val item = new JSONObject
			item.put(kv._1, kv._2)
			headerArray.put(item)
		})
		val contentArray = new JSONArray
		tableBody.put("Content", contentArray)
		listOfObj.foreach(l => {
			val item = new JSONObject
			item.put("Tuple", l.toString)
			contentArray.put(item)
		})
		json
	}

	private[this] def addIdentifier(input:String):String ={
		val result = ZKPersistenceUtil.addOn.stripMargin.concat("\n").concat(input)
		result
	}



	protected def realValue(updateVal: (String, String)): Object = {
		val (field, newValue) = updateVal
		this.context.tableMetadata.get(field) match {
			case Some(DataTypes.String)  => newValue
			case Some(DataTypes.Int)     => Integer.valueOf(newValue)
			case Some(DataTypes.Boolean) => java.lang.Boolean.valueOf(newValue)
			case Some(other)             => throw DmlBadSyntaxException("unsupported data type: " + field + "," + other)
			case None                    => throw DmlNoSuchFieldException(field)
		}
	}

	//update records in java bean, please note the jave bean inside TableInfoMap() also changed and include new value
	protected def updateExecutionContext(newRecords: List[Object]): Unit = {
		import scala.collection.JavaConverters._
		val javaList: java.util.List[Object] = newRecords.asJava
		invokeSetByReflection(this.context.tabledata, TUPLE_FIELD, javaList)
	}


	def doExecute(): Unit

	def execute(): Unit = {
		zkCacheRWLock.writeLockTable(this.table)
		try {
			doExecute()
			ZKPersistenceUtil.persist(this.table, addIdentifier(toJson(this.records).toString(3)))
			updateExecutionContext(this.records)
		} finally {
			zkCacheRWLock.writeUnLockTable(this.table)
		}
	}

	def executeDebug(): Unit = {
		doExecute()
		updateExecutionContext(this.records)
		println(toJson(this.records).toString(3))
	}
}