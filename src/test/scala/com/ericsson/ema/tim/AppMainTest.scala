package com.ericsson.ema.tim

import com.ericsson.ema.tim.zookeeper.{ZKConnectionManager, ZKMonitor}

/**
  * Created by eqinson on 2017/5/13.
  */
object AppMainTest extends App {
	val zkm = ZKConnectionManager()
	zkm.init()
	val zkMonitor = new ZKMonitor(zkm)
	zkMonitor.start()

	while (!Thread.currentThread.isInterrupted)
		try
			Thread.sleep(1000 * 60)
		catch {
			case e: InterruptedException =>
		}

	zkMonitor.stop()
	zkm.destroy()
}
