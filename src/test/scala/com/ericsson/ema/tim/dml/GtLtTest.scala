package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.{BiggerThan, LessThan, Like}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class GtLtTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[GtLtTest])

	"Test1" should "pass gt lt test" in {
		LOGGER.info("=====================select some data for testing gt lt=====================")
		val result: List[List[Object]] = Select("name", "age", "job").from(tableName).where(Like("name",
			"eqinson[0-9]")).where(BiggerThan("age", 3)).
			where(LessThan("age", 7)).orderBy("name", "asc").orderBy("age", "desc").
			orderBy("job").collectBySelectFields()
		TestUtil.printResult(result)
	}
}

