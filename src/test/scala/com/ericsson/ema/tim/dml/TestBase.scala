package com.ericsson.ema.tim.dml

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
  * Created by eqinson on 2017/5/13.
  */
trait TestBase extends FlatSpec with Matchers with BeforeAndAfterAll {
	protected val tableName = "Eqinson"
	private[this] val testFile = "test.json"

	override def beforeAll(): Unit = {
		TestUtil.init(testFile, tableName)
	}
}
