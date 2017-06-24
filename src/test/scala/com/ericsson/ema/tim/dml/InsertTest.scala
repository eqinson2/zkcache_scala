package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/6/23.
  */
class InsertTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[InsertTest])

	"Test1" should "pass insert test" in {
		LOGGER.info("=====================insert some data=====================")
		Insert().into(tableName).add("name", "eqinson111").add("age", "1234")
			.add("job", "software engineer").add("hometown", "SH").add("maintenance", "TRUE").executeDebug()

		val result = Select().from(tableName).where(Eq("name", "eqinson111")).where(Eq("age", "1234")).collect()
		result.foreach(println)

		intercept[DmlBadSyntaxException] {
			Insert().into(tableName).add("name", "eqinson1").add("age", "1").add("job", "software engineer")
				.add("hometown", "SH").add("maintenance", "TRUE").executeDebug()
		}

		intercept[DmlBadSyntaxException] {
			Insert().into(tableName).add("name", "eqinson1").add("age", "1").add("job", "software engineer")
				.add("hometown", "SH").executeDebug()
		}
	}
}

