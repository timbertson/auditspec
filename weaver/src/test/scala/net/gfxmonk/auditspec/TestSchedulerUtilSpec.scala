package net.gfxmonk.auditspec

import monix.eval.Task
import weaver.monixcompat.SimpleTaskSuite

import scala.concurrent.duration._

object TestSchedulerUtilSpec extends SimpleTaskSuite with TestSchedulerSuite {
  val testTask = for {
    startTime <- Task(System.currentTimeMillis())
    _ <- Task.sleep(1.second)
    endTime <- Task(System.currentTimeMillis())
  } yield {
    expect(endTime - startTime < 500)
  }

  pureTest("explicit run method") {
    TestSchedulerUtil.run(testTask)
  }

  syncTest("syncTest utility") {
    testTask
  }
}
