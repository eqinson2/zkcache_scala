package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Range
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class CountTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[CountTest])

	"Test1" should "pass count test" in {
		LOGGER.info("=====================select some data for testing count=====================")
		println(Select("name", "age", "job").from(tableName).where(Range("age", 4, 6)).count())
	}
}