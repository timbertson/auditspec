package net.gfxmonk.auditspec.weaver

import monix.eval.Task
import net.gfxmonk.auditspec.TestSchedulerUtil
import monix.execution.schedulers.TestScheduler
import weaver.monixcompat.MutableTaskSuite
import weaver.{Expectations, TestName}

import scala.concurrent.Await
import scala.concurrent.duration._

trait TestSchedulerSuite { self: MutableTaskSuite =>
  def syncTest(name: TestName)(body : => Task[Expectations]) :  Unit = pureTest(name)(TestSchedulerUtil.run(body))
}
