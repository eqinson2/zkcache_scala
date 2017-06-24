lazy val `sbt-release` = project in file(".")

name := "zkcache"
organization := "com.ericsson"
scalaVersion := "2.11.7"

// This forces the compiler to create a jar for this project and include that in the classpath
// If not set the compiler will use the classes directly
// This is needed in order to easily copy all jars when creating the tar.gz
exportJars := true

// Needed to make sbt release work when use sbt 0.13+
updateOptions := updateOptions.value.withCachedResolution(!Option(System.getProperty("skipwithCachedResolution")).isDefined)


//---------------------------------------
// Compiler directives
//---------------------------------------

// allow circular dependencies for test sources
compileOrder in Test := CompileOrder.Mixed

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-language:higherKinds", "-target:jvm-1.8")

import org.dmonix.sbt.ScalaDocSettings

scalacOptions in(Compile, doc) ++= ScalaDocSettings.rootDoc

//this setting overrides the default sequence of files to add in packageDoc
//it's needed in order for the copyDocAssetsTask task to execute
mappings in(Compile, packageDoc) <<= copyDocAssetsTask

//---------------------------------------
// Configure the needed settings to be able to publish artifacts to the binary repository (e.g. ARM)
// Settings:
// * The credentials to use when publishing artifacts
// * The URL where to deploy to
// * The URL to our own Nexus/ARM servers
//---------------------------------------
import com.ericsson.activation.sbt.plugin.ARMSettings
import org.dmonix.sbt.CredentialsSettings

credentials ++= CredentialsSettings.publishCredentials
publishTo <<= version {
	ARMSettings.deployURL(_)
}
resolvers ++= ARMSettings.resolverURLs

//---------------------------------------
// packageModule related information
//---------------------------------------
productNumber := "CXC1737820"
installationFiles := "installation"
//adds the generated tar.gz to files that shall be uploaded/published
import com.ericsson.activation.sbt.plugin.ModuleTarPlugin._

addArtifact(name {
	moduleArtifact(_)
}, packageModuleTar)


//---------------------------------------

libraryDependencies ++= Seq(
	"org.slf4j" % "slf4j-log4j12" % "1.7.5" % "provided",
	"log4j" % "log4j" % "1.2.16" % "provided",
	"org.json" % "json" % "20160810",
	"javassist" % "javassist" % "3.12.1.GA",
	"org.apache.zookeeper" % "zookeeper" % "3.4.5" % "provided",
	"com.ericsson" % "zookeeper-utils" % "3.3.1" % "provided",
	"org.scalatest" %% "scalatest" % "2.2.6" % "test"
)


// this disables appending the scala version to the produced binary when deployed to Nexus
crossPaths := false

// enable publishing the main jar produced by `package`
publishArtifact in(Compile, packageBin) := true

// enable publishing the main API jar
publishArtifact in(Compile, packageDoc) := true

// enable publishing the main sources jar
publishArtifact in(Compile, packageSrc) := true
