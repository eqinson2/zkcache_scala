package com.ericsson.ema.tim.pojo

/**
  * Created by eqinson on 2017/5/8.
  */
case class NameType(theName: String, theType: String) {
	override def toString: String = "NameType{" + "name='" + theName + '\'' + ", type='" + theType + '\'' + '}'
}

class TableTuple(theName: String, theType: String) extends NameType(theName: String, theType: String) {
	var tuples: List[NameType] = List[NameType]()

	override def toString: String = {
		super.toString + "\n" + "TableTuple{" + "tuples=" + tuples.map(_.toString + "\n").reduce(_ + "\n" + _) + '}'
	}
}

case class Table(name: String, records: TableTuple) {
	override def toString: String = "Table{" + "name='" + name + '\'' + ", records=" + records + '}'
}
