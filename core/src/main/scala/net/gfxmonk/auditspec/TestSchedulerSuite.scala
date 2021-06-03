package net.gfxmonk.auditspec

import cats.effect.Resource
import monix.eval.Task

//abstract class TestSchedulerSuite
//  extends EffectSuite[Task]
//  with Expectations.Helpers {
//
//  private val testScheduler = TestScheduler()
//
//  implicit protected def effectCompat = new TestSchedulerCompat(testScheduler)
//
//  def pureTest(name: String)(run: => Expectations): Task[TestOutcome] =
//    Test[Task](name, Task(run))
//  def simpleTest(name: String)(run: Task[Expectations]): Task[TestOutcome] =
//    Test[Task](name, run)
//  def loggedTest(name: String)(
//    run: Log[Task] => Task[Expectations]): Task[TestOutcome] =
//    Test[Task](name, run)
//}

abstract class TestSchedulerSuite extends MutableTestSchedulerSuite {
  override type Res = Unit
  override def sharedResource: Resource[Task, Res] = Resource.pure(())
}
