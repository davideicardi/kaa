// used to automatic calculate version based on git describe
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

// used to publish artifact to maven central via sonatype nexus
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.4")
// used to sign the artifcat with pgp keys
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")