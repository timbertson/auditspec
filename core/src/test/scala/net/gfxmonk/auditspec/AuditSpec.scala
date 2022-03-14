package net.gfxmonk.auditspec

import weaver.SimpleIOSuite
import cats.implicits._
import cats.effect.IO

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

object AuditSpec extends SimpleIOSuite {
  private val resource = Audit.resource[IO, String]

  test("record / reset / get") {
    resource.use { audit =>
      for {
        _ <- audit.record("a")
        _ <- audit.record("b")
        _ <- audit.record("c")
        initialEvents <- audit.reset
        _ <- audit.record("d")
        _ <- audit.record("e")
        _ <- audit.record("f")
        finalEvents <- audit.get
      } yield {
        expect.all(
          initialEvents == List("a", "b", "c"),
          finalEvents == List("d", "e", "f"))
      }
    }
  }

  test("waitUntil") {
    resource.use { audit =>
      for {
        _ <- audit.record("initial")
        initialCheck <- audit.waitUntil(_.size > 1).timeout(1.millis).attempt.map(res => res.left.toOption)
        finalCheck <- IO.race(
          IO.sleep(10.millis) >> audit.record("second") >> IO.never,
          audit.waitUntil(_.size > 1)
        )
      } yield {
        expect.all(
          initialCheck.map(_.getClass) == Some(classOf[TimeoutException]),
          finalCheck == Right(List("initial", "second")))
      }
    }
  }

  def testPartitioning(name: String, events: List[String], spec: List[List[String]], expected: List[List[String]]) = pureTest(s"partitionAndSort: $name") {
    val (partitioned, _) = Audit.partitionAndSort(events, spec)
    expect(partitioned == expected, name)
  }

  testPartitioning("excess events",
    events = List("a", "b", "c"),
    spec = List(List("_", "_")),
    expected = List(List("a", "b"), List("c")))

  testPartitioning("insufficient events (within a set)",
    events = List("a", "b", "c"),
    spec = List(List("_", "_"), List("_", "_")),
    expected = List(List("a", "b"), List("c")))

  testPartitioning("insufficient sets",
    events = List("a", "b"),
    spec = List(List("_", "_"), List("_", "_")),
    expected = List(List("a", "b")))

  testPartitioning("matching events",
      events = List("a", "aa", "b", "c"),
      spec = List(List("_", "_"), List("_"), List("_")),
      expected = List(List("a", "aa"), List("b"), List("c")))

  testPartitioning("same length",
    events = List("a", "b", "c"),
    spec = List(List("_", "_"), List("_")),
    expected = List(List("a", "b"), List("c")))

  testPartitioning("unsorted",
    events = List("b", "a", "c"),
    spec = List(List("_", "_"), List("_")),
    expected = List(List("a", "b"), List("c")))
}
