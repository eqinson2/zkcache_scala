package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.dml.predicate.Eq
import org.slf4j.LoggerFactory

/**
  * Created by eqinson on 2017/5/12.
  */
class EqTest extends TestBase {
	private[this] val LOGGER = LoggerFactory.getLogger(classOf[EqTest])

	"Test1" should "pass eq test" in {
		LOGGER.info("=====================select some data for testing eq=====================")
		var result: List[Object] = Select().from(tableName).where(Eq("name", "eqinson1")).where(Eq("age", "1")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(Eq("name", "eqinson2")).where(Eq("age", "6")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(Eq("name", "eqinson1")).where(Eq("age", "4")).where(Eq("job", "manager")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(Eq("maintenance", "TRUE")).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(Eq("name", "eqinson1")).where(Eq("maintenance", "TRUE")).skip(1).collect()
		result.foreach(println)
		println
		result = Select().from(tableName).where(Eq("maintenance", "FALSE")).limit(1).collect()
		result.foreach(println)
		println
		val result2: List[List[Object]] = Select("name", "age", "job", "maintenance").from(tableName).where(Eq("name", "eqinson1")).where(Eq("maintenance", "FALSE")).collectBySelectFields()
		TestUtil.printResult(result2)
		println
		val result3: List[List[Object]] = Select("name", "age", "job", "maintenance").from(tableName).collectBySelectFields()
		TestUtil.printResult(result3)
		println
	}
}
