package com.ericsson.ema.tim

import java.net.InetAddress

import com.ericsson.ema.tim.context.{Tab2MethodInvocationCacheMap, TableInfoContext, TableInfoMap}
import com.ericsson.ema.tim.dml.Insert
import com.ericsson.ema.tim.exception.DmlBadSyntaxException
import com.ericsson.ema.tim.reflection.{AccessType, MethodInvocationCache}
import com.ericsson.ema.tim.zookeeper.{ZKConnectionManager, ZKMonitor, ZKPersistenceUtil}

import scala.util.Try

/**
  * Created by eqinson on 2017/5/13.
  */
object AppMainTest extends App {
	private[this] val testFile = "test.json"
	var context: TableInfoContext = _
	var methodInvocationCache: MethodInvocationCache = _
	protected val TUPLE_FIELD = "records"
	protected var table: String = "Eqinson"
	protected var records: List[Object] = _

	val zkm = ZKConnectionManager()
	zkm.init()

	val in = getClass.getResourceAsStream("/" + testFile)
	val lines = scala.io.Source.fromInputStream(in).getLines.mkString("\n")
	ZKPersistenceUtil.persist("Eqinson",addIdentifier(lines))

	val zkMonitor = new ZKMonitor(zkm)
	zkMonitor.start()

//	initExecuteContext()
//load success
//	records.foreach(println)

	//do a change
//	ZKPersistenceUtil.persist("Eqinson",addIdentifier(lines))

	Insert().into("Eqinson").add("name", "eqinson111").add("age", "1234")
		.add("job", "software engineer").add("hometown", "SH").add("maintenance", "TRUE").execute()

	while (!Thread.currentThread.isInterrupted)
		try
			Thread.sleep(1000 * 60)
		catch {
			case e: InterruptedException =>
		}

	zkMonitor.stop()
	zkm.destroy()

	private[this] def addIdentifier(input:String):String ={
		import java.lang.management.ManagementFactory
		val pid:String = ManagementFactory.getRuntimeMXBean.getName
		val addr: InetAddress = InetAddress.getLocalHost;
		val addOn = pid +"@"+ addr.getHostName + "endHead"
		val result = addOn.stripMargin.concat("\n").concat(input)
		result
	}

	protected def initExecuteContext(): Unit = {
		//context is TableInfoContext(tableData, tableMetadata)
		 context = TableInfoMap().lookup(table).getOrElse(throw DmlBadSyntaxException("Error: Selecting a " + "non-existing table:" + table))
		 methodInvocationCache = Tab2MethodInvocationCacheMap().lookup(table)
		//it is safe because records must be List according to JavaBean definition
		val tupleField = invokeGetByReflection(context.tabledata, TUPLE_FIELD)
		import scala.collection.JavaConversions._
		records = tupleField.asInstanceOf[java.util.List[Object]].toList
	}

	private[this] def invokeGetByReflection(obj: Object, wantedField: String): Object = {
		val getter = methodInvocationCache.get(obj.getClass, wantedField, AccessType.GET)
		Try(getter.invoke(obj)).getOrElse(throw DmlBadSyntaxException("invokeGetByReflection error!"))
	}
}

