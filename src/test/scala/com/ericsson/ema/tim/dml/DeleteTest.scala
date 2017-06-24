package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/6/23.
  */
class DeleteTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[DeleteTest])

	"Test1" should "pass delete test" in {
		LOGGER.info("=====================delete some data=====================")
		Delete().from(tableName).where(Eq("name", "eqinson1")).where(Eq("age", "1")).where(Eq("job", "software engineer"))
			.where(Eq("hometown", "SH")).where(Eq("maintenance", "TRUE")).executeDebug()

		intercept[DmlBadSyntaxException] {
			Delete().from(tableName).where(Eq("name", "eqinson1111")).where(Eq("age", "1")).where(Eq("job", "software engineer"))
				.where(Eq("hometown", "SH")).where(Eq("maintenance", "TRUE")).executeDebug()
		}

		intercept[DmlBadSyntaxException] {
			Delete().from(tableName).where(Eq("name", "eqinson1111")).where(Eq("age", "1")).where(Eq("job", "software engineer"))
				.where(Eq("hometown", "SH")).executeDebug()
		}
	}
}

