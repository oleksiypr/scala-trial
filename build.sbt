name := "scala-trial"

version := "0.1.0"

scalaVersion := "3.4.2"

libraryDependencies ++= Seq(
  "org.typelevel"      %% "cats-effect"         % "3.5.2",
  "org.http4s"         %% "http4s-core"         % "0.23.25",
  "org.http4s"         %% "http4s-dsl"          % "0.23.25",
  "org.http4s"         %% "http4s-ember-server" % "0.23.25",
  "org.http4s"         %% "http4s-ember-client" % "0.23.25",
)

libraryDependencies ++= Seq(
  "org.scalatest"      %% "scalatest"        % "3.2.18"   % Test,
  "org.scalacheck"     %% "scalacheck"       % "1.17.0"   % Test,
  "org.scalatestplus"  %% "mockito-4-11"     % "3.2.18.0" % Test,
  "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.7.0" % Test,
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

Test / parallelExecution := false
