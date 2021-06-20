package net.gfxmonk.auditspec

import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import weaver.monixcompat.MutableTaskSuite
import weaver.{Expectations, TestName}

import scala.concurrent.Await
import scala.concurrent.duration._

object TestSchedulerUtil {
  def runWith[T](testScheduler: TestScheduler)(task: Task[T]): T = {
    val future = task.runToFuture(testScheduler)
    while(!future.isCompleted) {
      testScheduler.tick(500.days)
    }
    Await.result(future, Duration.Zero)
  }

  def run[T](task: Task[T]): T = {
    runWith(TestScheduler())(task)
  }
}

trait TestSchedulerUtil { self: MutableTaskSuite =>
  def syncTest(name: TestName)(body : => Task[Expectations]) :  Unit = pureTest(name)(TestSchedulerUtil.run(body))
}
