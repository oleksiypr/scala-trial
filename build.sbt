name := "scala-trial"

version := "0.1.0"

scalaVersion := "3.8.1"

scalacOptions ++= Seq(
  "-Vprint:postInlining",
  "-Xmax-inlines:100000",
  "-Xcheck-macros",
)

libraryDependencies ++= Seq(
  "org.typelevel"      %% "cats-effect"         % "3.5.2",
  "org.http4s"         %% "http4s-core"         % "0.23.25",
  "org.http4s"         %% "http4s-dsl"          % "0.23.25",
  "org.http4s"         %% "http4s-ember-server" % "0.23.25",
  "org.http4s"         %% "http4s-ember-client" % "0.23.25",
  "org.http4s"         %% "http4s-circe"        % "0.23.26",
  "io.circe"           %% "circe-core"          % "0.14.7",
  "io.circe"           %% "circe-generic"       % "0.14.7",
  "io.circe"           %% "circe-parser"        % "0.14.7",
  "io.circe"           %% "circe-literal"       % "0.14.7",
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

Test / parallelExecution := true
