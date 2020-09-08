lazy val commonSettings = Seq(
  organization := "your-app",
  version := "0.1",
  scalaVersion := "2.13.3"
)

val avro4sVersion = "4.0.0"
val kafkaVersion = "2.4.0" // NOTE: there is a dependencies to kafka also from avro4s-kafka

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
)

val dependencies = Seq(
  "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion,
  "com.sksamuel.avro4s" %% "avro4s-kafka" % avro4sVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "com.github.blemale" %% "scaffeine" % "4.0.1",
)

lazy val KaaSchemaRegistry = project
  .settings(
    name := "KaaSchemaRegistry",
    commonSettings,
    libraryDependencies ++= testDependencies,
    libraryDependencies ++= dependencies,
  )

lazy val SampleApp = project
  .settings(
    name := "SampleApp",
    commonSettings,
    libraryDependencies ++= testDependencies,
  )
.dependsOn(KaaSchemaRegistry)

lazy val root = (project in file("."))
  .aggregate(KaaSchemaRegistry, SampleApp)
  .settings(
    commonSettings
  )