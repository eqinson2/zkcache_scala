package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.{Like, Range, UnLike}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class RangeTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[EqTest])

	"Test1" should "pass range test" in {
		LOGGER.info("=====================select some data for testing range=====================")
		val result: List[List[Object]] = Select("name", "age", "job").from(tableName).where(Like("name", "eqinson[0-9]")).
			where(UnLike("job", "engineer")).where(Range("age", 1, 60)).orderBy("name", "asc")
			.orderBy("age", "desc").orderBy("job").collectBySelectFields()
		TestUtil.printResult(result)
		println
	}
}

