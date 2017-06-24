package com.ericsson.ema.tim.dml.group

import com.ericsson.ema.tim.dml.{DataTypes, SelectClause}
import com.ericsson.ema.tim.exception.{DmlBadSyntaxException, DmlNoSuchFieldException}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class GroupBy(protected override val field: String) extends SelectClause {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[GroupBy])

	private[this] type keyExtractorFuncType = Object => Object

	def keyExtractor(): keyExtractorFuncType = {
		operator.context.tableMetadata.get(field) match {
			case Some(DataTypes.String) | Some(DataTypes.Int) | Some(DataTypes.Boolean) =>
				getFiledValFromTupleByName
			case Some(other)                                                            =>
				LOGGER.error("unsupported data type: {},{}", field, other: Any)
				throw DmlBadSyntaxException("Error: unsupported data type: " + field + "," + other)
			case None                                                                   =>
				throw DmlNoSuchFieldException(field)

		}
	}
}


