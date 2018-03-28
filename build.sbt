name := "stockpile"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.4"

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
)

lazy val core = (project in file("core"))
   .settings(
     libraryDependencies ++= Seq(
       "org.typelevel" %% "cats-core" % "1.0.1",
       "org.typelevel" %% "cats-effect" % "0.5",
       "org.scalatest" %% "scalatest" % "3.0.4" % "test",
     )
   )

