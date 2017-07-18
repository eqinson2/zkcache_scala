package com.ericsson.ema.tim.reflection

import java.beans.Introspector

import com.ericsson.ema.tim.context.{Tab2ClzMap, Tab2MethodInvocationCacheMap}
import com.ericsson.ema.tim.dml.DataTypes
import com.ericsson.ema.tim.exception.{DmlBadSyntaxException, DmlNoSuchFieldException}
import com.ericsson.ema.tim.json.{FieldInfo, JsonLoader}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/5/10.
  */
case class TabDataLoader(private val classToLoad: String, private val jloader: JsonLoader) {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[TabDataLoader])

	private[this] val TUPLE_FIELD = "records"
	//cache:MethodInvocationCache, create when first time load a table
	private[this] val cache = Tab2MethodInvocationCacheMap().lookup(jloader.tableName)

	private[this] def realFieldVal(field: FieldInfo): Object = {
		field.fieldType match {
			case DataTypes.String  => field.fieldValue
			case DataTypes.Int     => java.lang.Integer.valueOf(field.fieldValue)
			case DataTypes.Boolean => java.lang.Boolean.valueOf(field.fieldValue)
			case _                 =>
				LOGGER.error("unsupported data type: {}", field.fieldValue)
				throw DmlNoSuchFieldException(field.fieldName)
		}
	}

	//records = List[EqinsonData]
	def loadData(): Object = {
		LOGGER.info("=====================reflect class: {}=====================", classToLoad)
		val clz = Tab2ClzMap().lookup(jloader.tableName).getOrElse(Thread.currentThread.getContextClassLoader.loadClass(classToLoad))
		Tab2ClzMap().register(jloader.tableName, clz)
		val obj = clz.newInstance
		val tupleListType = loadTupleClz(obj)
		LOGGER.debug("init {}", tupleListType)
		//getter:java.lang.reflect.Method
		val getter = cache.get(clz, TUPLE_FIELD, AccessType.GET)
		val records = getter.invoke(obj).asInstanceOf[java.util.List[Object]]
		jloader.tupleList.foreach(row => {
			//new instance of ***Data like EqinsonData
			val tuple = tupleListType.newInstance.asInstanceOf[Object]
			//field is FieldInfo, row is List[FieldInfo].size == column size
			row.foreach(field => TabDataLoaderUtil.fillInField(tuple, field.fieldName, realFieldVal(field)))
			records.add(tuple)
		})
		obj.asInstanceOf[Object]
	}


	private[this] def loadTupleClz(instance: Any): Class[_] = {
		val tupleClassName = instance.getClass.getName + "Data"
		//must use same classloader as PojoGen
		LOGGER.info("=====================load class: {}=====================", tupleClassName)
		instance.getClass.getClassLoader.loadClass(tupleClassName)
	}
}

object TabDataLoaderUtil {
	private[this] val LOGGER = LoggerFactory.getLogger(TabDataLoaderUtil.getClass)

	def fillInField(tuple: Object, field: String, value: Object): Unit = {
		val beanInfo = Introspector.getBeanInfo(tuple.getClass)
		val propertyDescriptors = beanInfo.getPropertyDescriptors
		propertyDescriptors.toList.filter(field == _.getName) match {
				//the first element in the list
			case h :: _ =>
				LOGGER.debug("fillInField : {} = {}", field, value: Any)
				val setter = h.getWriteMethod

				Try(setter.invoke(tuple, value)) match {
					case Success(_)  =>
					case Failure(ex) => LOGGER.error("error fillInField : {}", field)
						throw DmlBadSyntaxException(ex.getMessage)
				}
			case _      =>
				LOGGER.error("should not happen.")
				throw new RuntimeException("bug in fillInField...")
		}
	}
}

