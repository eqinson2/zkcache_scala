package com.ericsson.ema.tim.exception

/**
  * Created by eqinson on 2017/5/5.
  */
case class DmlBadSyntaxException(error: String) extends RuntimeException(error: String)

case class DmlNoSuchFieldException(field: String) extends RuntimeException("Error: No such field " + field)
