package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.{Eq, Like, UnEq, UnLike}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class LikeTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[LikeTest])

	"Test1" should "pass like test" in {
		LOGGER.info("=====================select some data for testing like/unlike=====================")
		var result: List[Object] = Select().from(tableName).where(Like("name", "eqinson[0-9]")).where(Eq("age", "1")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnLike("name", "eqinson[0-9]")).where(UnEq("age", "6")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(Like("job", "^HR+ admin+$")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnLike("job", "^HR|.*admin$")).collect()
		result.foreach(println)
		println
		val result2: List[List[Object]] = Select("name", "age", "job").from(tableName).where(Like("name", "eqinson[0-9]")).where(UnLike("job", ".*engineer$")).orderBy("name", "asc").orderBy("age", "desc").orderBy("job").collectBySelectFields()
		TestUtil.printResult(result2)
		println
	}
}
