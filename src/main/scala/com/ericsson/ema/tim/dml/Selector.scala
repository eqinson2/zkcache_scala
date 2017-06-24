package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.PredicateClause

/**
  * Created by eqinson on 2017/5/12.
  */
trait Selector {
	def from(tab: String): Selector

	def where(predicate: PredicateClause): Selector

	def limit(limit: Int): Selector

	def skip(skip: Int): Selector

	def collect(): List[Object]

	def collectBySelectFields(): List[List[Object]]

	def count(): Long

	def exists(): Boolean

	def orderBy(field: String, asc: String = "asc"): Selector

	def groupBy(field: String): Selector

	def collectByGroup(): Map[Object, List[Object]]
}
