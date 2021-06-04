package net.gfxmonk.auditspec

import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import weaver.monixcompat.SimpleTaskSuite

import scala.concurrent.duration._

object TestSchedulerUtilSpec extends SimpleTaskSuite {
  val testTask = for {
    startTime <- Task(System.currentTimeMillis())
    _ <- Task.sleep(1.second)
    endTime <- Task(System.currentTimeMillis())
  } yield {
    expect(endTime - startTime < 500)
  }

  pureTest("execution occurs in TestScheduler") {
    TestSchedulerUtil.run { (scheduler: TestScheduler) =>
      testTask
    }
  }
}

object TestSchedulerSuiteSpec extends SimpleTestSchedulerSuite {
  test("execution occurs in TestScheduler") {
    TestSchedulerUtilSpec.testTask
  }
}
