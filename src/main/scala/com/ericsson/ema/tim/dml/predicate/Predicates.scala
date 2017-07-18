package com.ericsson.ema.tim.dml.predicate

import com.ericsson.ema.tim.exception.DmlBadSyntaxException

/**
  * Created by eqinson on 2017/5/20.
	* Eq override the mathcher function.
	* (l, r) =>   l is the feild value, r is the value to be compared, which is always String
  */
class Eq private(override val field: String, protected override val valueToComp: Object) extends PredicateClause {
	protected override val StringMatcher: matcherFuncType = _ == _
	protected override val IntMatcher: matcherFuncType = (l, r) => l.asInstanceOf[Integer] == Integer.valueOf(r.asInstanceOf[String])
	protected override val BoolMatcher: matcherFuncType = (l, r) => l.asInstanceOf[java.lang.Boolean] == java.lang.Boolean.valueOf(r.asInstanceOf[String])
}

object Eq {
	def apply(field: String, value: String) = new Eq(field, value)
}

class UnEq private(protected override val field: String, protected override val valueToComp: Object) extends PredicateClause {
	protected override val StringMatcher: matcherFuncType = _ != _
	protected override val IntMatcher: matcherFuncType = (l, r) => l.asInstanceOf[Integer] != Integer.valueOf(r.asInstanceOf[String])
	protected override val BoolMatcher: matcherFuncType = (l, r) => l.asInstanceOf[java.lang.Boolean] != java.lang.Boolean.valueOf(r.asInstanceOf[String])
}

object UnEq {
	def apply(field: String, value: String) = new UnEq(field, value)
}

class Like private(protected override val field: String, protected override val valueToComp: Object) extends PredicateClause {
	protected override val StringMatcher: matcherFuncType = (l, r) => l.asInstanceOf[String].matches(r.asInstanceOf[String])
}

object Like {
	def apply(field: String, value: String) = new Like(field, value)
}

class UnLike private(protected override val field: String, protected override val valueToComp: Object) extends PredicateClause {
	protected override val StringMatcher: matcherFuncType = (l, r) => !l.asInstanceOf[String].matches(r.asInstanceOf[String])
}

object UnLike {
	def apply(field: String, value: String) = new UnLike(field, value)
}

class BiggerThan private(protected override val field: String, protected override val valueToComp: Object) extends PredicateClause {
	protected override val IntMatcher: matcherFuncType = (l, r) => l.asInstanceOf[Integer].compareTo(r.asInstanceOf[Integer]) > 0
}

object BiggerThan {
	def apply(field: String, value: Int) = new BiggerThan(field, Integer.valueOf(value))
}

class LessThan private(protected override val field: String, protected override val valueToComp: Object) extends PredicateClause {
	protected override val IntMatcher: matcherFuncType = (l, r) => l.asInstanceOf[Integer].compareTo(r.asInstanceOf[Integer]) < 0
}

object LessThan {
	def apply(field: String, value: Int) = new LessThan(field, Integer.valueOf(value))
}

class Range private(override val field: String, from: Int, to: Int) extends PredicateClause {
	if (from > to) throw DmlBadSyntaxException("from must be lte to")
	protected override val IntMatcher: matcherFuncType = (l, _) => l.asInstanceOf[Integer].compareTo(from) >= 0 && l.asInstanceOf[Integer].compareTo(to) < 0
	//valueToComp not used here, so assign a AnyRef
	protected override val valueToComp: Object = new AnyRef
}

object Range {
	def apply(field: String, from: Int, to: Int) = new Range(field, from, to)
}