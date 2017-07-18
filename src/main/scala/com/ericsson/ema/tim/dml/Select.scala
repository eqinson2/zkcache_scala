package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.group.GroupBy
import com.ericsson.ema.tim.dml.order.{ChainableOrderings, OrderBy}
import com.ericsson.ema.tim.dml.predicate.PredicateClause
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock

/**
  * Created by eqinson on 2017/5/12.
  */
class Select private() extends Operator with Selector with ChainableOrderings {
	private[this] val TUPLE_FIELD: String = "records"

	private[this] var selectedFields: List[String] = List[String]()
	private[this] var predicates = List[PredicateClause]()
	private[this] var orderBys = List[OrderBy]()
	private[this] var groupBy: GroupBy = _
	private[this] var limit = Integer.MIN_VALUE
	private[this] var skip = Integer.MIN_VALUE

	def this(fields: String*) {
		this()
		this.selectedFields = if (fields.nonEmpty) fields.toList else selectedFields
	}

	override def from(tab: String): Selector = {
		this.table = tab
		this
	}

	override def where(predicate: PredicateClause): Selector = {
		this.predicates :+= predicate
		predicate.asInstanceOf[SelectClause].operator = this
		this
	}

	override def limit(limit: Int): Selector = {
		if (limit <= 0) throw DmlBadSyntaxException("Error: limit must be > 0")
		this.limit = limit
		this
	}

	override def skip(skip: Int): Selector = {
		if (skip <= 0) throw DmlBadSyntaxException("Error: skip must be > 0")
		this.skip = skip
		this
	}

	override def collect(): List[Object] = {
		if (selectedFields.nonEmpty)
			throw DmlBadSyntaxException("Error: must use collectBySelectFields if some fields are to be selected")

		zkCacheRWLock.readLockTable(table)
		try {
			internalExecute()
		} finally {
			zkCacheRWLock.readUnLockTable(table)
		}
	}

	override def collectBySelectFields(): List[List[Object]] = {
		if (selectedFields.isEmpty)
			throw DmlBadSyntaxException("Error: Must use execute if full fields are to be selected")

		zkCacheRWLock.readLockTable(table)
		try {
			for (obj <- internalExecute())
				yield selectedFields.map(invokeGetByReflection(obj, _)).foldRight(List[Object]())(_ :: _)
		} finally {
			zkCacheRWLock.readUnLockTable(table)
		}
	}

	private[this] def internalExecute(): List[Object] = {
		//get all records java.util.List[Object] from java bean
		initExecuteContext()
		var result = records
		if (predicates.nonEmpty)
			//the input r is the instance of java bean
			result = records.filter(internalPredicate())
		if (orderBys.nonEmpty)
			result = result.sorted(orderBys.map(_.ordering()).reduce(_ thenOrdering _))
		if (skip > 0)
			result = result.drop(skip)
		if (limit > 0)
			result = result.take(limit)
		result
	}

	private[this] def internalPredicate(): Object => Boolean = {
//r => eqs.forall(_ eval r)
		r => predicates.map(_.eval(r)).reduce(_ && _)
	}

	override def count(): Long = {
		if (limit != Integer.MIN_VALUE || skip != Integer.MIN_VALUE)
			throw DmlBadSyntaxException("Error: meaningless to specify skip/limit in count.")

		zkCacheRWLock.readLockTable(table)
		try {
			initExecuteContext()
			records.count(internalPredicate())
		} finally {
			zkCacheRWLock.readUnLockTable(table)
		}
	}

	override def exists(): Boolean = {
		if (limit != Integer.MIN_VALUE || skip != Integer.MIN_VALUE)
			throw DmlBadSyntaxException("Error: meaningless to specify skip/limit in exists.")

		zkCacheRWLock.readLockTable(table)
		try {
			initExecuteContext()
			records.exists(internalPredicate())
		} finally {
			zkCacheRWLock.readUnLockTable(table)
		}
	}

	override def orderBy(field: String, asc: String = "asc"): Selector = {
		val o = OrderBy(field, asc)
		this.orderBys :+= o
		o.operator = this
		this
	}

	override def groupBy(field: String): Selector = {
		Option(groupBy) match {
			case None    =>
				val g = new GroupBy(field)
				this.groupBy = g
				g.operator = this
				this
			case Some(_) => throw DmlBadSyntaxException("Error: only support one groupBy Clause")
		}
	}

	override def collectByGroup(): Map[Object, List[Object]] = {
		zkCacheRWLock.readLockTable(table)
		try {
			Option(groupBy) match {
				case Some(_) => collect().groupBy(groupBy.keyExtractor())
				case None    => throw DmlBadSyntaxException("Error: must specify groupBy when using collectByGroup.")
			}
		} finally {
			zkCacheRWLock.readUnLockTable(table)
		}
	}


}

object Select {
	def apply() = new Select

	def apply(fields: String*) = new Select(fields: _*)
}