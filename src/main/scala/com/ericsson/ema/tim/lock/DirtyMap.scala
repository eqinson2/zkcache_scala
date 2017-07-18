package com.ericsson.ema.tim.lock

import scala.collection.immutable.Map

object DirtyMap {
  private[this] var dirtyMap: Map[String, Boolean] = Map[String, Boolean]()

  def isDirty(zkNodeName: String): Boolean = {
    dirtyMap.get(zkNodeName) match {
      case Some(dirty) => dirty
      case None => false
    }
  }

  def setDirty(zkNodeName: String): Unit = {
    dirtyMap += (zkNodeName -> true)
  }

  def clearDirty(zkNodeName: String): Unit = {
    dirtyMap += (zkNodeName -> false)
  }
}