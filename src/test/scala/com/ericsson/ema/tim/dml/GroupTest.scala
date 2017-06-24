package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Range
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class GroupTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[GroupTest])

	"Test1" should "pass groupby test" in {
		LOGGER.info("=====================select some data for testing groupby=====================")
		val res = Select().from(tableName).where(Range("age", 1, 10)).groupBy("name").collectByGroup()
		TestUtil.printResultGroup(res)

		val res1 = Select().from(tableName).groupBy("maintenance").collectByGroup()
		TestUtil.printResultGroup(res1)
	}
}