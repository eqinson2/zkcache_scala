import sbt.Keys._
import sbt._

object Commons {

  //---------------------------------------
  // Distribution settings
  //---------------------------------------
  val settings: Seq[Def.Setting[_]] = Seq(
    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    }
  )
}
