
scalaVersion in ThisBuild := "2.13.5"

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "net.gfxmonk",
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  testFrameworks += new TestFramework("weaver.framework.Monix"),
)

lazy val core = (project in file("core")).settings(
  commonSettings,
  name := "interaction-log",

  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.3.0",
    "org.typelevel" %% "cats-effect" % "2.3.0",
    "io.monix" %% "monix" % "3.2.2",
    "com.disneystreaming" %% "weaver-cats" % "0.6.3",
    "com.disneystreaming" %% "weaver-monix" % "0.6.3",
  ),
)

lazy val root = (project in file("."))
  .aggregate(core)
