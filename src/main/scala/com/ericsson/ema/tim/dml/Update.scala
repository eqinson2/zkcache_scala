package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/6/23.
  */
class Update extends ChangeOperator {
	private val LOGGER = LoggerFactory.getLogger(classOf[Update])

	private var eqs: List[Eq] = List[Eq]()
	private var updateFields: List[(String, String)] = List[(String, String)]()

	def into(tab: String): Update = {
		this.table = tab
		this
	}

	def where(eq: Eq): Update = {
		this.eqs :+= eq
		eq.operator = this
		this
	}

	def set(field: String, value: String): Update = {
		updateFields :+= (field, value)
		this
	}

	override def doExecute(): Unit = {
		if (updateFields.isEmpty)
			throw DmlBadSyntaxException("Error: missing updateFields and addFields!")

		val isEmpty = eqs.foldLeft(Select().from(this.table))(_ where _).collect().isEmpty

		initExecuteContext()
		zkCacheRWLock.readLockTable(this.table)
		try {
			this.records = cloneList(this.records)
		} finally {
			zkCacheRWLock.readUnLockTable(this.table)
		}

		if (!isEmpty)
			for (obj <- this.records if eqs.forall(_ eval obj); update <- updateFields)
				invokeSetByReflection(obj, update._1, realValue(update))
		else
			throw DmlBadSyntaxException("Error: Update a record which doesn't exists!")
	}
}

object Update {
	def apply() = new Update
}
