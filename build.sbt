val stockpileVersion = "0.1.0"

val defaultSettings = Seq(
  scalaVersion := "2.12.4",

  scalacOptions in ThisBuild ++= Seq(
    "-language:higherKinds",
    "-deprecation",
    "-unchecked",
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
  .aggregate(core, cli)


val circeVersion = "0.9.3"
val catsVersion = "1.1.0"
val catsEffectVersion = "0.10.1"
val scalatestVersion = "3.0.4"
val akkaVersion = "2.5.12"
val http4sVersion = "0.18.11"

lazy val core = module("core")
   .configs(IntegrationTest)
   .settings(
     Defaults.itSettings,
     libraryDependencies ++= Seq(
       "org.typelevel" %% "cats-core" % catsVersion,
       "org.typelevel" %% "cats-effect" % catsEffectVersion,

       "com.github.pureconfig" %% "pureconfig" % "0.9.1",

       "io.circe" %% "circe-core" % circeVersion,
       "io.circe" %% "circe-generic" % circeVersion,
       "io.circe" %% "circe-parser" % circeVersion,

       // Pricing
       "org.http4s" %% "http4s-dsl" % http4sVersion,
       "org.http4s" %% "http4s-blaze-server" % http4sVersion,
       "org.http4s" %% "http4s-blaze-client" % http4sVersion,
       "org.http4s" %% "http4s-circe" % http4sVersion,
       "com.github.pureconfig" %% "pureconfig-http4s" % "0.9.1",
       "io.lemonlabs" %% "scala-uri" % "0.4.16", // TODO: old outdated version, but the API has changed since then

       // Inventory
       "com.nrinaudo" %% "kantan.csv" % "0.4.0", // csv parser + writer

       "org.scalatest" %% "scalatest" % scalatestVersion % "it,test",
     ),
  )

lazy val cli = module("cli")
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",

      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    ),
  ).dependsOn(core)