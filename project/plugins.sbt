
//Either Nexus or ARM is needed to get the ema-module-sbt-plugin.
val localNexus = "http://sekalx366.epk.ericsson.se:8081/nexus/content"
resolvers ++= Seq(
	"Local Nexus Snapshots" at localNexus + "/repositories/snapshots/",
	"Local Nexus Releases" at localNexus + "/repositories/releases/"
)
val pluginRepo = "https://arm.epk.ericsson.se/artifactory/"
resolvers ++= Seq(
	"ARM Snapshots" at pluginRepo + "/proj-ema-dev-local",
	"ARM Releases" at pluginRepo + "/proj-ema-release-local",
	"JBoss" at "https://repository.jboss.org/"
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.1.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0")

addSbtPlugin("com.ericsson.activation" % "activation-sbt-plugins" % "0.5")

addSbtPlugin("org.dmonix.sbt" % "sbt-scaladoc-settings-plugin" % "0.7")
addSbtPlugin("org.dmonix.sbt" % "sbt-publish-settings-plugin" % "0.5")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")