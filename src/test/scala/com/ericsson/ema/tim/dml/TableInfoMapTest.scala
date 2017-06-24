package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.context.TableInfoMap
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
  * Created by eqinson on 2017/5/20.
  */
class TableInfoMapTest extends FlatSpec with Matchers with BeforeAndAfterAll {
	"Test1" should "pass" in {
		assert(TableInfoMap() eq TableInfoMap())
		assert(TableInfoMap() == TableInfoMap())
	}
}
