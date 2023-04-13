// NOTE: This file is generated by chored

import scala.util.Try

ThisBuild / scalaVersion := "2.13.7"
ThisBuild / organization := "net.gfxmonk"
sonatypeProfileName := "net.gfxmonk"

ThisBuild / version := {
	def make(v: String, snapshot: Boolean) = if (snapshot) v + "-SNAPSHOT" else v
	def isSnapshot: Try[Boolean] = sys.env.get("SNAPSHOT").map {
		case "true" => true
		case "false" => false
		// NOTE: this is an abort, not a Try.Failure
		case other => throw new RuntimeException(s"Invalid $$SNAPSHOT value: $other")
	}.toRight(new RuntimeException("$SNAPSHOT required")).toTry

	def fileVersion = {
		// from file, we assume snapshot since that's the dev env
		val base = Try(IO.read(new File("VERSION")).trim()).toOption
		base.map { v => make(v, isSnapshot.getOrElse(true)) }
	}
	def envVersion = {
		// from env, we require $SNAPSHOT to be set as well
		sys.env.get("VERSION").map(v => make(v, isSnapshot.get))
	}

	envVersion.orElse(fileVersion).getOrElse(make("0.0.0", true))
}

ThisBuild / homepage := Some(url(s"https://github.com/timbertson/auditspec"))
ThisBuild / scmInfo := Some(
	ScmInfo(
		url("https://github.com/timbertson/auditspec"),
		s"scm:git@github.com:timbertson/auditspec.git"
	)
)

credentials += Credentials(
	"Sonatype Nexus Repository Manager",
	"oss.sonatype.org",
	"timbertson",
	sys.env.getOrElse("SONATYPE_PASSWORD", "******"))

ThisBuild / licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.php"))

ThisBuild / developers := List(
	Developer(
		id    = "gfxmonk",
		name  = "Tim Cuthbertson",
		email = "tim@gfxmonk.net",
		url   = url("http://gfxmonk.net")
	)
)
