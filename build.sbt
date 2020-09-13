import xerial.sbt.Sonatype._

lazy val commonSettings = Seq(
  organization := "com.davideicardi",
  scalaVersion := "2.13.3",
  scalacOptions += "-deprecation",
  // publish to sonatype/maven central
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  credentials += Credentials("Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    "davide.icardi",
    System.getenv("SONATYPE_PASSWORD")),
  licenses := Seq("MIT License" -> url("https://mit-license.org/")),
  sonatypeProjectHosting := Some(GitHubHosting("davideicardi", "kaa", "davide.icardi@gmail.com")),
  useGpgPinentry := true,
  // sbt-git version settings
  git.useGitDescribe := true,
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
    commonSettings,
    libraryDependencies ++= testDependencies,
    libraryDependencies ++= dependencies,
  )
  .enablePlugins(GitVersioning)

lazy val SampleApp = project
  .settings(
    name := "SampleApp",
    commonSettings,
    publish / skip := true,
  )
  .enablePlugins(GitVersioning)
  .dependsOn(KaaSchemaRegistry)

lazy val root = (project in file("."))
  .aggregate(KaaSchemaRegistry, SampleApp)
  .settings(
    commonSettings,
    publish / skip := true,
  )