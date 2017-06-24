package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/6/23.
  */
class UpdateTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[UpdateTest])

	"Test1" should "pass update test" in {
		LOGGER.info("=====================update some data=====================")
		Update().into(tableName).set("name", "eqinson100000000000").set("age", "100").where(Eq("name", "eqinson1")).where(Eq("age", "1"))
			.where(Eq("job", "software engineer")).where(Eq("hometown", "SH")).where(Eq("maintenance", "TRUE"))
			.executeDebug()

		val result = Select().from(tableName).where(Eq("name", "eqinson100000000000")).where(Eq("age", "100")).collect()
		result.foreach(println)

		intercept[DmlBadSyntaxException] {
			Update().into(tableName).set("age", "100").where(Eq("name", "eqinson111")).where(Eq("age", "1"))
				.where(Eq("job", "software engineer")).where(Eq("hometown", "SH")).where(Eq("maintenance", "TRUE"))
				.executeDebug()
		}
	}
}

