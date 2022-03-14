package net.gfxmonk.auditspec

import cats.effect.IO
import weaver.SimpleIOSuite

import scala.concurrent.duration._

object TestControlSuiteSpec extends SimpleIOSuite with TestControlSuite {
  val testTask = for {
    startTime <- IO(System.currentTimeMillis())
    _ <- IO.sleep(1.second)
    endTime <- IO(System.currentTimeMillis())
  } yield {
    expect(endTime - startTime < 500)
  }

  controlTest("controlTest utility") {
    testTask
  }
}
