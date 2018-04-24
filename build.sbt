import sbt.Keys.{name, publishArtifact, scalacOptions}

val stockpileVersion = "0.1.0"

val defaultSettings = Seq(
  scalaVersion := "2.12.4",

  scalacOptions in ThisBuild ++= Seq(
    "-language:higherKinds",
    "-deprecation",
    "-feature",
  ),
)

def module(moduleName: String): Project = Project(moduleName, file(moduleName))
  .settings(defaultSettings)
  .settings(
    name := s"stockpile-$moduleName",
    version := stockpileVersion,
    resolvers += Resolver.jcenterRepo,
    licenses in ThisBuild += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayRepository := "oss-maven",
    bintrayReleaseOnPublish in ThisBuild := false
  )

lazy val root = (project in file("."))
  .settings(defaultSettings)
  .settings(
    skip in publish := true,
    // workaround for https://github.com/sbt/sbt-bintray/issues/93, fill be fixed in 0.5.4
    publishArtifact := false,
    bintrayRelease := (),
    bintrayEnsureBintrayPackageExists := (),
    bintrayEnsureLicenses := ()
  )


val circeVersion = "0.9.3"
val catsVersion = "1.1.0"
val catsEffectVersion = "0.10.1"
val scalatestVersion = "3.0.4"
val akkaVersion = "2.5.12"

lazy val core = module("core")
   .settings(
     libraryDependencies ++= Seq(
       "org.typelevel" %% "cats-core" % catsVersion,
       "org.typelevel" %% "cats-effect" % catsEffectVersion,

       "io.circe" %% "circe-core" % circeVersion,
       "io.circe" %% "circe-generic" % circeVersion,
       "io.circe" %% "circe-parser" % circeVersion,

       // Inventory
       "com.nrinaudo" %% "kantan.csv" % "0.4.0", // csv parser + writer

       "org.scalatest" %% "scalatest" % scalatestVersion % Test,
     ),
  )

lazy val cli = module("cli")
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    ),
  ).dependsOn(core)