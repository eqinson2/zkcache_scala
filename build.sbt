lazy val `sbt-release` = project in file(".")

name := "zkcache"
organization := "com.ericsson.ema"
scalaVersion := "2.11.7"

// This forces the compiler to create a jar for this project and include that in the classpath
// If not set the compiler will use the classes directly
// This is needed in order to easily copy all jars when creating the tar.gz
exportJars := true

autoScalaLibrary in ThisBuild := false

// Needed to make sbt release work when use sbt 0.13+
updateOptions := updateOptions.value.withCachedResolution(!Option(System.getProperty("skipwithCachedResolution")).isDefined)

//disablePlugins(ModuleTarPlugin)

//---------------------------------------
// Compiler directives
//---------------------------------------
crossPaths in ThisBuild := false

// allow circular dependencies for test sources
compileOrder in Test := CompileOrder.Mixed

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-language:higherKinds", "-target:jvm-1.8")

//---------------------------------------
// Configure the needed settings to be able to publish artifacts to the binary repository (e.g. ARM)
// Settings:
// * The credentials to use when publishing artifacts
// * The URL where to deploy to
// * The URL to our own Nexus/ARM servers
//---------------------------------------
import com.ericsson.activation.sbt.plugin.ARMSettings
import org.dmonix.sbt.CredentialsSettings

publishArtifact in ThisBuild := false
publishArtifact := true

credentials ++= CredentialsSettings.publishCredentials
publishTo := ARMSettings.deployURL(version.value)
resolvers in ThisBuild ++= ARMSettings.resolverURLs

val testAndCompile = "test->test;compile->compile"
// A configuration which is like 'compile' except it performs additional static analysis.
// Execute static analysis via `lint:compile`
val LintTarget = config("lint").extend(Compile)

libraryDependencies ++= Seq(
	"org.slf4j" % "slf4j-log4j12" % "1.7.5" % "provided",
	"log4j" % "log4j" % "1.2.16" % "provided",
	"org.json" % "json" % "20160810",
	"javassist" % "javassist" % "3.12.1.GA",
	"org.apache.zookeeper" % "zookeeper" % "3.4.5" % "provided",
	"com.ericsson" % "zookeeper-utils" % "3.3.1" % "provided",
	"org.scalatest" %% "scalatest" % "2.2.6" % "test"
)


lazy val `zkcache`: Project = project.in(file("."))
	.settings(Commons.settings: _*)
	.disablePlugins(ModuleTarPlugin)
	.configs(LintTarget)
