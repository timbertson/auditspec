package net.gfxmonk.auditspec

import cats.effect.{ContextShift, Resource, Timer}
import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import monix.execution.{Cancelable, Scheduler}
import weaver.monixcompat.BaseTaskSuite
import weaver.{Expectations, MutableFSuite, UnsafeRun}

abstract class MutableTestSchedulerSuite
  extends MutableFSuite[Task]
    with BaseTaskSuite
    with Expectations.Helpers {

  val testScheduler = TestScheduler()

  implicit protected val effectCompat = new TestSchedulerCompat(testScheduler)
}

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


abstract class SimpleTestSchedulerSuite extends MutableTestSchedulerSuite {
  override type Res = Unit
  override def sharedResource: Resource[Task, Res] = Resource.pure(())
}

private[auditspec] class TestSchedulerCompat(testScheduler: TestScheduler) extends UnsafeRun[Task] {
  type CancelToken = Cancelable

  private val realScheduler = Scheduler.global

  override implicit val contextShift: ContextShift[Task] = Task.contextShift(testScheduler)
  override implicit val timer: Timer[Task] = Task.timer(realScheduler)
  override implicit val effect = Task.catsEffect(testScheduler)
  override implicit val parallel = Task.catsParallel

  def background(task: Task[Unit]): Cancelable = {
    println("background...")
    task.runAsync { _ => () }(realScheduler)
  }

  def cancel(token: CancelToken): Unit = token.cancel()

  def sync(task: Task[Unit]): Unit = {
    println("sync (test)")
    TestSchedulerUtil.runWith(testScheduler)(_ => task)
  }

  def async(task: Task[Unit]): Unit = {
    println("async")
    task.runAsyncAndForget(realScheduler)
  }
}
