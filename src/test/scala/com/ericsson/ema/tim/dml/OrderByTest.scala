package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/12.
  */
class OrderByTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[OrderByTest])

	"Test1" should "pass orderby test" in {
		LOGGER.info("=====================select some data for testing orderby=====================")
		var result: List[Object] = Select().from(tableName).where(Eq("name", "eqinson2")).orderBy("age").orderBy("job", "desc").collect()
		result.foreach(println)
		System.out.println()

		result = Select().from(tableName).orderBy("hometown", "desc").orderBy("job").collect()
		result.foreach(println)
		System.out.println()
	}
}

