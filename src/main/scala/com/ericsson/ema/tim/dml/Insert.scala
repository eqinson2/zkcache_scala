package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.context.Tab2ClzMap
import com.ericsson.ema.tim.dml.predicate.Eq
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock
import com.ericsson.ema.tim.pojo.PojoGenerator
import com.ericsson.ema.tim.reflection.TabDataLoaderUtil
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/6/23.
  */
class Insert private() extends ChangeOperator {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[Insert])

	private[this] var addFields: List[(String, String)] = List[(String, String)]()

	def into(tab: String): Insert = {
		this.table = tab
		this
	}

	def add(field: String, value: String): Insert = {
		addFields :+= (field, value)
		this
	}

	private[this] def validateAddOperation(): Boolean = {
		val listOfUpdateFields: List[String] = addFields.map(_._1)
		val listOfTableFields: List[String] = this.context.tableMetadata.keySet.toList
		listOfUpdateFields.forall(listOfTableFields contains _) && listOfTableFields.forall(listOfUpdateFields contains _)
	}

	private[this] def makeNewRecord() = {
		val clz = Tab2ClzMap().lookup(this.table).getOrElse(Thread.currentThread.getContextClassLoader.loadClass(PojoGenerator.pojoPkg + "." + this.table))
		val obj = clz.newInstance
		val tuple = loadTupleClz(obj).newInstance.asInstanceOf[Object]
		addFields.foreach(f => TabDataLoaderUtil.fillInField(tuple, f._1, realValue(f)))
		tuple
	}

	private[this] def loadTupleClz(instance: Any): Class[_] = {
		val tupleClassName = instance.getClass.getName + "Data"
		//must use same classloader as PojoGen
		LOGGER.info("=====================load class: {}=====================", tupleClassName)
		instance.getClass.getClassLoader.loadClass(tupleClassName)
	}

	override def doExecute(): Unit = {
		if (addFields.isEmpty)
			throw DmlBadSyntaxException("Error: missing addFields!")

		val isEmpty = addFields.foldLeft(Select().from(this.table))((c, f) => c.where(Eq(f._1, f._2))).collect().isEmpty

		initExecuteContext()

		zkCacheRWLock.writeLockTable(this.table)
		try {
			this.records = cloneList(this.records)
		} finally {
			zkCacheRWLock.writeUnLockTable(this.table)
		}

		if (isEmpty) {
			if (!validateAddOperation())
				throw DmlBadSyntaxException("Error: Not add all table fields")
			this.records :+= makeNewRecord()
		}
		else
			throw DmlBadSyntaxException("Error: Insert a record which already exists!")
	}
}

object Insert {
	def apply() = new Insert
}
