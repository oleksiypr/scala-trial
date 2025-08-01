name := "scala-trial"

version := "0.1.0"

scalaVersion := "3.4.2"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

Test / parallelExecution := false 