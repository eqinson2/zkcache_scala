package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import com.ericsson.ema.tim.reflection.AccessType

import scala.util.Try

/**
  * Created by eqinson on 2017/5/12.
  */
trait SelectClause {
	private[tim] var operator: Operator = _

	protected val field: String

	protected def getFiledValFromTupleByName(tuple: Object): Object = {
		val getter = operator.methodInvocationCache.get(tuple.getClass, field, AccessType.GET)
		Try(getter.invoke(tuple)).getOrElse(throw DmlBadSyntaxException("getFiledValFromTupleByName error!"))
	}
}
