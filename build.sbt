import ScalaProject._

val weaverVersion = "0.7.11"
val catsVersion = "3.3.7"

val weaverCats = "com.disneystreaming" %% "weaver-cats" % weaverVersion

ThisBuild / versionScheme := Some("semver-spec")

lazy val commonSettings = Seq(
  organization := "net.gfxmonk",
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect-std" % catsVersion,
    "org.typelevel" %% "cats-effect-testkit" % catsVersion % Test,
    weaverCats % Test
  ),
)

lazy val core = (project in file("core")).settings(
  commonSettings,
  publicProjectSettings,
  name := "auditspec",
)

lazy val weaver = (project in file("weaver")).settings(
  commonSettings,
  publicProjectSettings,
  name := "auditspec-weaver",

  libraryDependencies ++= Seq(
    weaverCats,
    "org.typelevel" %% "cats-effect-testkit" % catsVersion,
  ),
).dependsOn(core)

lazy val example = (project in file("example")).settings(
  commonSettings,
  hiddenProjectSettings,
  name := "auditspec",
).dependsOn(core, weaver)

lazy val root = (project in file("."))
  .settings(
    name := "auditspec-root",
    hiddenProjectSettings
  )
  .aggregate(core, weaver, example)
