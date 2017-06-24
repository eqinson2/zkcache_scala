package com.ericsson.ema.tim.dml

import com.ericsson.ema.tim.zookeeper.ZKMonitor

/**
  * Created by eqinson on 2017/5/12.
  */
object TestUtil {
	def init(testFile: String, tableName: String): Unit = {
		val in = getClass.getResourceAsStream("/" + testFile)
		val lines = scala.io.Source.fromInputStream(in).getLines.mkString("\n")
		val zm = new ZKMonitor(null)
		zm.doLoad(tableName, lines)
	}

	def printResult(sliceRes: List[List[Object]]): Unit = {
		for (eachRow <- sliceRes) {
			if (eachRow.isInstanceOf[List[_]]) {
				val row = eachRow.asInstanceOf[List[Object]]
				row.foreach((r: Object) => print(r + "   "))
			}
			println
		}
	}

	def printResultGroup(mapRes: Map[Object, List[Object]]): Unit = {
		mapRes.foreach(kv => {
			if (Option(kv._2).isDefined) {
				val row = kv._2.asInstanceOf[List[Object]]
				row.foreach((r: Object) => print(r + "   "))
			}
			println
		})
	}
}
