package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate._
import com.ericsson.ema.tim.exception.{DmlBadSyntaxException, DmlNoSuchFieldException}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class ExceptionTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[ExceptionTest])

	"Test1" should "pass ExceptionTest" in {
		LOGGER.info("=====================select some data for testing exists=====================")
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Eq("name1", "eqinson")).where(Like("job", "engineer")).where(Range("age", 1, 10)).groupBy("name").collectByGroup()
		}
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Like("name", "eqinson")).where(Like("job1", "engineer")).where(Range("age", 1, 10)).groupBy("name").collectByGroup()
		}
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Range("age1", 1, 10)).groupBy("name").collectByGroup()
		}
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Like("name", "eqinson")).where(UnLike("job1", "engineer")).where(Range("age", 1, 10)).groupBy("name").collectByGroup()
		}
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Like("name", "eqinson")).where(UnLike("job", "engineer")).where(BiggerThan("age1", 1)).groupBy("name").collectByGroup()
		}
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Like("name", "eqinson")).where(UnLike("job", "engineer")).where(LessThan("age1", 10)).groupBy("name").collectByGroup()
		}
		intercept[DmlNoSuchFieldException] {
			Select().from(tableName).where(Like("name", "eqinson")).where(Like("job", "engineer")).where(Range("age", 1, 10)).groupBy("name1").collectByGroup()
		}

		LOGGER.info("=====================select some data for testing groupby with " + "DmlBadSyntaxException=====================")
		intercept[DmlBadSyntaxException] {
			Select().from(tableName).where(Like("name", "eqinson")).where(Like("job", "engineer")).where(Range("age", 1, 6)).groupBy("name").groupBy("age").collectByGroup()
		}
	}
}