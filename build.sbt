import xerial.sbt.Sonatype._

// Common settings
organization in ThisBuild := "com.davideicardi"
scalaVersion in ThisBuild := "2.13.3"
scalacOptions in ThisBuild += "-deprecation"

// sbt-dynver version settings
dynverSonatypeSnapshots in ThisBuild := true

lazy val publishSettings = Seq(
  // publish to sonatype/maven central
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    "davide.icardi",
    System.getenv("SONATYPE_PASSWORD")
  ),
  licenses := Seq("MIT License" -> url("https://mit-license.org/")),
  sonatypeProjectHosting := Some(GitHubHosting("davideicardi", "kaa", "davide.icardi@gmail.com")),
  useGpgPinentry := Option(System.getenv("PGP_PASSPHRASE")).isDefined, // set pinentry=loopback if we have the env variable
)

val avro4sVersion = "4.0.0"
val kafkaVersion = "2.4.0" // NOTE: there is a dependencies to kafka also from avro4s-kafka

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.0" % "it,test",
)

val dependencies = Seq(
  "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion exclude("org.slf4j", "slf4j-api"),
  "com.sksamuel.avro4s" %% "avro4s-kafka" % avro4sVersion exclude("org.slf4j", "slf4j-api"),
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "com.github.blemale" %% "scaffeine" % "4.0.1",
)

lazy val KaaSchemaRegistry = project
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    name := "kaa",
    publishSettings,
    libraryDependencies ++= testDependencies,
    libraryDependencies ++= dependencies,
  )

lazy val SampleApp = project
  .settings(
    name := "SampleApp",
    publish / skip := true,
  )
  .dependsOn(KaaSchemaRegistry)

lazy val root = (project in file("."))
  .aggregate(KaaSchemaRegistry, SampleApp)
  .settings(
    publish / skip := true,
  )