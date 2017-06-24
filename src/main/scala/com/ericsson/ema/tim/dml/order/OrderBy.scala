package com.ericsson.ema.tim.dml.order

import com.ericsson.ema.tim.dml.{DataTypes, SelectClause}
import com.ericsson.ema.tim.exception.{DmlBadSyntaxException, DmlNoSuchFieldException}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/12.
  */
class OrderBy private(protected override val field: String, reversed: Boolean) extends SelectClause {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[OrderBy])

	def ordering(): Ordering[Object] = {
		operator.context.tableMetadata.get(field) match {
			case Some(DataTypes.String) if !reversed =>
				new Ordering[Object] {
					def compare(o1: Object, o2: Object): Int = {
						val s1 = getFiledValFromTupleByName(o1).asInstanceOf[String]
						val s2 = getFiledValFromTupleByName(o2).asInstanceOf[String]
						s1 compareTo s2
					}
				}

			case Some(DataTypes.String) if reversed =>
				new Ordering[Object] {
					def compare(o1: Object, o2: Object): Int = {
						val s1 = getFiledValFromTupleByName(o1).asInstanceOf[String]
						val s2 = getFiledValFromTupleByName(o2).asInstanceOf[String]
						s2 compareTo s1
					}
				}

			case Some(DataTypes.Int) if !reversed =>
				new Ordering[Object] {
					def compare(o1: Object, o2: Object): Int = {
						val s1 = getFiledValFromTupleByName(o1).asInstanceOf[Integer]
						val s2 = getFiledValFromTupleByName(o2).asInstanceOf[Integer]
						s1 compareTo s2
					}
				}

			case Some(DataTypes.Int) if reversed =>
				new Ordering[Object] {
					def compare(o1: Object, o2: Object): Int = {
						val s1 = getFiledValFromTupleByName(o1).asInstanceOf[Integer]
						val s2 = getFiledValFromTupleByName(o2).asInstanceOf[Integer]
						s2 compareTo s1
					}
				}

			case Some(DataTypes.Boolean) if !reversed =>
				new Ordering[Object] {
					def compare(o1: Object, o2: Object): Int = {
						val s1 = getFiledValFromTupleByName(o1).asInstanceOf[java.lang.Boolean]
						val s2 = getFiledValFromTupleByName(o2).asInstanceOf[java.lang.Boolean]
						s1 compareTo s2
					}
				}

			case Some(DataTypes.Boolean) if reversed =>
				new Ordering[Object] {
					def compare(o1: Object, o2: Object): Int = {
						val s1 = getFiledValFromTupleByName(o1).asInstanceOf[java.lang.Boolean]
						val s2 = getFiledValFromTupleByName(o2).asInstanceOf[java.lang.Boolean]
						s2 compareTo s1
					}
				}

			case Some(other) =>
				LOGGER.error("bug: unsupported data type: {},{}", field, other: Any)
				throw DmlBadSyntaxException("Error: unsupported data type: " + field + "," + other)

			case None => throw DmlNoSuchFieldException(field)
		}

	}
}

object OrderBy {
	def apply(field: String, asc: String): OrderBy = {
		if (asc.toUpperCase == "DESC") new OrderBy(field, true)
		else if (asc.toUpperCase == "ASC") new OrderBy(field, false)
		else throw DmlBadSyntaxException("Error: orderBy must be either asc or desc")
	}

	def apply(field: String): OrderBy = {
		new OrderBy(field, false)
	}
}
