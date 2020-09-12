// used to automatic calculate version based on git describe
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

// used to publish artifact to maven central via sonatype nexus
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.4")
// used to sign the artifcat with pgp keys
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")