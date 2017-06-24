package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Range
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class ExistsTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[ExistsTest])

	"Test1" should "pass exist test" in {
		LOGGER.info("=====================select some data for testing exists=====================")
		println(Select("name", "age", "job").from(tableName).where(Range("age", 4, 6)).exists())
	}
}
