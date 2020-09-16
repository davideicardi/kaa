import xerial.sbt.Sonatype._

// Common settings
organization in ThisBuild := "com.davideicardi"
scalaVersion in ThisBuild := "2.13.3"
scalacOptions in ThisBuild ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
)

// sbt-dynver version settings
dynverSonatypeSnapshots in ThisBuild := true

// publish to sonatype/maven central
publishTo in ThisBuild := sonatypePublishToBundle.value
publishMavenStyle in ThisBuild := true
credentials in ThisBuild += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  "davide.icardi",
  System.getenv("SONATYPE_PASSWORD")
)
licenses in ThisBuild := Seq("MIT License" -> url("https://mit-license.org/"))
homepage in ThisBuild := Some(url("https://github.com/davideicardi/kaa"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/davideicardi/kaa"),
    "scm:git@github.com:davideicardi/kaa.git"
  )
)
developers in ThisBuild := List(
  Developer(id="davideicardi", name="Davide Icardi", email="davide.icardi@gmail.com", url=url("http://davideicardi.com"))
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
    // set pinentry=loopback if we have the env variable, to avoid asking for passphrase
    useGpgPinentry := Option(System.getenv("PGP_PASSPHRASE")).isDefined,
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