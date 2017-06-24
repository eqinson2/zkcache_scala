package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.{Eq, UnEq}
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/13.
  */
class UnEqTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[UnEqTest])

	"Test1" should "pass uneq test" in {
		LOGGER.info("=====================select some data for testing uneq=====================")
		var result: List[Object] = Select().from(tableName).where(UnEq("name", "eqinson1")).where(Eq("age", "1")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnEq("name", "eqinson2")).where(UnEq("age", "6")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnEq("name", "eqinson1")).where(UnEq("age", "4")).where(UnEq("job", "manager")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnEq("maintenance", "TRUE")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnEq("name", "eqinson1")).where(UnEq("maintenance", "TRUE")).skip(1).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(UnEq("maintenance", "FALSE")).limit(1).collect()
		result.foreach(println)
		println
		val result2: List[List[Object]] = Select("name", "age", "job", "maintenance").from(tableName).where(UnEq("name", "eqinson1")).where(Eq("maintenance", "FALSE")).collectBySelectFields()
		TestUtil.printResult(result2)
		println
	}

}
