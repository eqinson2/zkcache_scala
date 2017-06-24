package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/6/23.
  */
class Delete private() extends ChangeOperator {
	private val LOGGER = LoggerFactory.getLogger(classOf[Delete])

	private var eqs: List[Eq] = List[Eq]()

	def from(tab: String): Delete = {
		this.table = tab
		this
	}

	def where(eq: Eq): Delete = {
		this.eqs :+= eq
		eq.operator = this
		this
	}

	private[this] def validateDeleteOperation(): Boolean = {
		val listOfDeleteFields: List[String] = eqs.map(_.field)
		val listOfTableFields: List[String] = this.context.tableMetadata.keySet.toList
		listOfDeleteFields.forall(listOfTableFields contains _) && listOfTableFields.forall(listOfDeleteFields contains _)
	}

	override def doExecute(): Unit = {
		val isEmpty = eqs.foldLeft(Select().from(this.table))(_ where _).collect().isEmpty

		initExecuteContext()
		if (!validateDeleteOperation()) { //ensure to delete on row each tme
			throw DmlBadSyntaxException("Error: Not specify all table fields in delete!")
		}
		zkCacheRWLock.readLockTable(this.table)
		try {
			this.records = cloneList(this.records)
		} finally {
			zkCacheRWLock.readUnLockTable(this.table)
		}
		if (!isEmpty)
			this.records = this.records.filterNot(r => eqs.forall(_ eval r))
		else
			throw DmlBadSyntaxException("Error: delete a record which doesn't exists!")
	}
}

object Delete {
	def apply() = new Delete
}