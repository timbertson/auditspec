package net.gfxmonk.auditspec

import monix.eval.Task
import weaver._
import weaver.monixcompat.MutableTaskSuite

trait TestSchedulerSuite {
  self: MutableTaskSuite =>
  def syncTest(name: TestName)(body: => Task[Expectations]): Unit = pureTest(name)(TestSchedulerUtil.run(body))
}
