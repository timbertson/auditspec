package net.gfxmonk.auditspec.example

import cats.effect.concurrent.{Deferred, Ref}
import cats.implicits._
import monix.eval.{Fiber, Task}

class SingletonProcessor(
  activeRef: Ref[Task, Option[Fiber[Unit]]],
  failureDeferred: Deferred[Task, Throwable],
) {
  def spawn(task: Task[Unit]) = {
    for {
      fiber <- task.onErrorHandleWith(failureDeferred.complete).start
      currentTask <- activeRef.getAndSet(Some(fiber))
      _ <- currentTask.traverse(_.cancel)
    } yield ()
  }

  private def failure: Task[Nothing] = failureDeferred.get.flatMap(Task.raiseError)

  private def cancel: Task[Unit] = activeRef.get.flatMap { maybeFiber => maybeFiber.traverse_(_.cancel) }
}

object SingletonProcessor {
  def use[T](block: SingletonProcessor => Task[T]): Task[T] = {
    for {
      fiberRef <- Ref[Task].of(Option.empty[Fiber[Unit]])
      failureDeferred <- Deferred[Task, Throwable]
      processor = new SingletonProcessor(fiberRef, failureDeferred)
      result <- Task.race(block(processor), processor.failure).flatMap {
        case Left(result) => Task.pure(result)
        case _: Right[T, Nothing] => Task.raiseError(new RuntimeException("Impossible (returned Nothing)"))
      }.guarantee(processor.cancel)
    } yield result
  }
}
