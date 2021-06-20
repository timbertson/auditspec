
scalaVersion in ThisBuild := "2.13.5"

val weaverVersion = "0.6.3"
val weaverCats = "com.disneystreaming" %% "weaver-cats" % weaverVersion
val weaverMonix = "com.disneystreaming" %% "weaver-cats" % weaverVersion

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "net.gfxmonk",
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  testFrameworks += new TestFramework("weaver.framework.Monix"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.3.0",
    "org.typelevel" %% "cats-effect" % "2.3.0",
    "io.monix" %% "monix" % "3.2.2",
    weaverCats % Test,
    weaverMonix % Test,
  ),
)

lazy val core = (project in file("core")).settings(
  commonSettings,
  name := "auditspec",
)

lazy val weaver = (project in file("weaver")).dependsOn(core).settings(
  commonSettings,
  name := "auditspec-weaver",

  libraryDependencies ++= Seq(
    weaverCats % Test,
    weaverMonix % Test,
  ),
)

lazy val root = (project in file("."))
  .aggregate(core, weaver)
