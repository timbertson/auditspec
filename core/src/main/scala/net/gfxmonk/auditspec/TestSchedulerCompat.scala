package net.gfxmonk.auditspec

import cats.effect.{ContextShift, Timer}
import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import monix.execution.{Cancelable, Scheduler}
import weaver.UnsafeRun

import scala.concurrent.Await
import scala.concurrent.duration._

private[auditspec] class TestSchedulerCompat(testScheduler: TestScheduler) extends UnsafeRun[Task] {
  println("instanced")
  type CancelToken = Cancelable

  private val realScheduler = Scheduler.global

  override implicit val contextShift: ContextShift[Task] = Task.contextShift(testScheduler)
  override implicit val timer: Timer[Task] = Task.timer(realScheduler)
  override implicit val effect = Task.catsEffect(testScheduler)
  override implicit val parallel = Task.catsParallel

  def background(task: Task[Unit]): Cancelable = {
    throw new RuntimeException("oof")
    println("background...")
    task.runAsync { _ => () }(realScheduler)
  }

  def cancel(token: CancelToken): Unit = token.cancel()

  def sync(task: Task[Unit]): Unit = {
    println("sync (test)")
    throw new RuntimeException("oof")
    val future = task.runToFuture(testScheduler)
    testScheduler.tick(9999.days)
    println(testScheduler.state)
    Await.result(future, 10.seconds)
  }

  def async(task: Task[Unit]): Unit = {
    println("async")
    throw new RuntimeException("oof")
    task.runAsyncAndForget(realScheduler)
  }
}
