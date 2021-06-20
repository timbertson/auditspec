package net.gfxmonk.auditspec.example

import monix.eval.Task
import net.gfxmonk.auditspec.{Audit, TestSchedulerUtil}
import weaver.Expectations
import weaver.monixcompat.SimpleTaskSuite

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ProcessorSpec extends SimpleTaskSuite with TestSchedulerUtil {
  class Context(val audit: Audit[String], val processor: SingletonProcessor) {
    val failure = new RuntimeException("simulated failure")

    def succeed(delay: FiniteDuration = Duration.Zero) = {
      audit.record("succeed: start") >>
        (Task.sleep(delay) >>
          audit.record("succeed: finish")
         ).doOnCancel(audit.record("succeed: cancel"))
    }

    def fail(delay: FiniteDuration = Duration.Zero) = {
      audit.record("fail: start") >>
        (Task.sleep(delay) >>
          audit.record("fail: finish") >>
          Task.raiseError(failure)
        ).doOnCancel(audit.record("fail: cancel"))
    }

    def finalLog = Task.sleep(1.day) >> audit.get
  }

  def ctxTest(desc: String)(block: Context => Task[Expectations]) = {
    syncTest(desc) {
      Audit.resource[String].use { audit =>
        SingletonProcessor.use { processor =>
          val ctx = new Context(audit, processor)
          block(ctx).flatMap { result =>
            Task.sleep(1.hour).as(result)
          }
        }
      }
    }
  }

  ctxTest("runs a task in the background") { ctx =>
    for {
      _ <- ctx.processor.spawn(ctx.succeed(10.seconds))
      _ <- Task.sleep(1.second)
      _ <- ctx.audit.record("foreground")
      log <- ctx.finalLog
    } yield {
      expect(log == List("succeed: start", "foreground", "succeed: finish"))
    }
  }

  ctxTest("cancels the current task when spawning a new one") { ctx =>
    for {
      _ <- ctx.processor.spawn(ctx.fail(10.seconds))
      _ <- Task.sleep(5.seconds)

      // delay task body so that cancellation propagates first
      _ <- ctx.processor.spawn(ctx.succeed().delayExecution(1.second))
      log <- ctx.finalLog
    } yield {
      expect(log == List(
        "fail: start",
        "fail: cancel",
        "succeed: start",
        "succeed: finish"))
    }
  }

  ctxTest("fails the entire block when a spawned task fails") { ctx =>
    for {
      result <- SingletonProcessor.use { processor =>
        processor.spawn(ctx.fail(10.seconds)) >>
          Task.sleep(1.second) >>
          ctx.audit.record("waiting") >>
          Task.sleep(20.seconds)
      }.materialize
      log <- ctx.finalLog
    } yield {
      expect(log == List(
        "fail: start",
        "waiting",
        "fail: finish")
      ).and(expect(result == Failure(ctx.failure)))
    }
  }

  ctxTest("cancels a running computation when the block completes") { ctx =>
    for {
      result <- SingletonProcessor.use { processor =>
        processor.spawn(ctx.succeed(10.seconds)) >>
          Task.sleep(1.second) >>
          ctx.audit.record("discarding")
      }.materialize
      log <- ctx.finalLog
    } yield {
      expect(log == List(
        "succeed: start",
        "discarding",
        "succeed: cancel")
      ).and(expect(result == Success()))
    }
  }
}