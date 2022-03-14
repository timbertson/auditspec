package net.gfxmonk.auditspec

import cats.effect.IO
import cats.effect.testkit.TestControl
import weaver._

trait TestControlSuite {
  self: MutableIOSuite =>
  def controlTest(name: TestName)(body: => IO[Expectations]): Unit = test(name) {
    TestControl.executeEmbed(body)
  }
}
