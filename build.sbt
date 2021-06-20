import ScalaProject._

scalaVersion in ThisBuild := "2.13.5"

val weaverVersion = "0.6.3"
val catsVersion = "2.3.0"

val weaverCats = "com.disneystreaming" %% "weaver-cats" % weaverVersion
val weaverMonix = "com.disneystreaming" %% "weaver-monix" % weaverVersion

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "net.gfxmonk",
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  testFrameworks += new TestFramework("weaver.framework.Monix"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "cats-effect" % catsVersion,
    "io.monix" %% "monix" % "3.2.2",
    weaverCats % Test,
    weaverMonix % Test,
  ),
)

lazy val core = (project in file("core")).settings(
  commonSettings,
  publicProjectSettings("auditspec"),
  name := "auditspec",
)

lazy val weaver = (project in file("weaver")).settings(
  commonSettings,
  publicProjectSettings("auditspec"),
  name := "auditspec-weaver",

  libraryDependencies ++= Seq(
    weaverCats,
    weaverMonix,
  ),
).dependsOn(core)

lazy val example = (project in file("example")).settings(
  commonSettings,
  hiddenProjectSettings,
  name := "auditspec",
).dependsOn(core, weaver)

lazy val root = (project in file("."))
  .aggregate(core, weaver, example)
