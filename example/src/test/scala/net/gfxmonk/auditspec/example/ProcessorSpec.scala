package net.gfxmonk.auditspec.example

import cats.effect.IO
import net.gfxmonk.auditspec.{Audit, TestControlSuite}
import weaver.{Expectations, SimpleIOSuite}

import scala.concurrent.duration._

object ProcessorSpec extends SimpleIOSuite with TestControlSuite {
  class Context(val audit: Audit[IO, String], val processor: SingletonProcessor) {
    val failure = new RuntimeException("simulated failure")

    def succeed(delay: FiniteDuration = Duration.Zero) = {
      audit.record("succeed: start") >>
        (IO.sleep(delay) >>
          audit.record("succeed: finish")
         ).onCancel(audit.record("succeed: cancel"))
    }

    def fail(delay: FiniteDuration = Duration.Zero) = {
      audit.record("fail: start") >>
        (IO.sleep(delay) >>
          audit.record("fail: finish") >>
          IO.raiseError(failure)
        ).onCancel(audit.record("fail: cancel"))
    }

    def finalLog = IO.sleep(1.day) >> audit.get
  }

  def ctxTest(desc: String)(block: Context => IO[Expectations]) = {
    controlTest(desc) {
      Audit.resource[IO, String].use { audit =>
        SingletonProcessor.use { processor =>
          val ctx = new Context(audit, processor)
          block(ctx).flatMap { result =>
            IO.sleep(1.hour).as(result)
          }
        }
      }
    }
  }

  ctxTest("runs a task in the background") { ctx =>
    for {
      _ <- ctx.processor.spawn(ctx.succeed(10.seconds))
      _ <- IO.sleep(1.second)
      _ <- ctx.audit.record("foreground")
      log <- ctx.finalLog
    } yield {
      expect(log == List("succeed: start", "foreground", "succeed: finish"))
    }
  }

  ctxTest("cancels the current task when spawning a new one") { ctx =>
    for {
      _ <- ctx.processor.spawn(ctx.fail(10.seconds))
      _ <- IO.sleep(5.seconds)

      // delay task body so that cancellation propagates first
      _ <- ctx.processor.spawn(ctx.succeed().delayBy(1.second))
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
          IO.sleep(1.second) >>
          ctx.audit.record("waiting") >>
          IO.sleep(20.seconds)
      }.attempt
      log <- ctx.finalLog
    } yield {
      expect(log == List(
        "fail: start",
        "waiting",
        "fail: finish")
      ).and(expect(result == Left(ctx.failure)))
    }
  }

  ctxTest("cancels a running computation when the block completes") { ctx =>
    for {
      result <- SingletonProcessor.use { processor =>
        processor.spawn(ctx.succeed(10.seconds)) >>
          IO.sleep(1.second) >>
          ctx.audit.record("discarding")
      }.attempt
      log <- ctx.finalLog
    } yield {
      expect(log == List(
        "succeed: start",
        "discarding",
        "succeed: cancel")
      ).and(expect(result == Right(())))
    }
  }
}