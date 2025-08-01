name := "scala-trial"

version := "0.1.0"

scalaVersion := "3.4.2"

// Add common dependencies
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
)

// Compiler options for Scala 3
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Wdead-code",
  "-Wnumeric-widen",
  "-Wvalue-discard"
)

// Test options
Test / parallelExecution := false 