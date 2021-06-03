package net.gfxmonk.auditspec

import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.concurrent.Await
import scala.concurrent.duration._

object TestSchedulerSpec {
  def run[T](block: TestScheduler => Task[T]): T = {
    val testScheduler = TestScheduler()
    val future = block(testScheduler).runToFuture(testScheduler)
    // ensure any plausible delay has been satisfied
    testScheduler.tick(500.days)
    Await.result(future, Duration.Zero)
  }
}
