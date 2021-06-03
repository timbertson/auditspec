package net.gfxmonk.auditspec

import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import weaver.{Expectations, MutableFSuite}
import weaver.monixcompat.BaseTaskSuite

abstract class MutableTestSchedulerSuite
  extends MutableFSuite[Task]
    with BaseTaskSuite
    with Expectations.Helpers {

  private val testScheduler = TestScheduler()

  implicit protected val effectCompat = new TestSchedulerCompat(testScheduler)
}
