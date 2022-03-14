package net.gfxmonk.auditspec.example

import cats.effect.{Deferred, IO}
import cats.effect.kernel.{Fiber, Ref}
import cats.implicits._

class SingletonProcessor(
  activeRef: Ref[IO, Option[Fiber[IO, Throwable, Unit]]],
  failureDeferred: Deferred[IO, Throwable],
) {
  def spawn(task: IO[Unit]) = {
    for {
      fiber <- task.onError(err => failureDeferred.complete(err).void).start
      currentTask <- activeRef.getAndSet(Some(fiber))
      _ <- currentTask.traverse(_.cancel)
    } yield ()
  }

  private def failure: IO[Nothing] = failureDeferred.get.flatMap(IO.raiseError)

  private def cancel: IO[Unit] = activeRef.get.flatMap { maybeFiber => maybeFiber.traverse_(_.cancel) }
}

object SingletonProcessor {
  def use[T](block: SingletonProcessor => IO[T]): IO[T] = {
    for {
      fiberRef <- Ref[IO].of(Option.empty[Fiber[IO, Throwable, Unit]])
      failureDeferred <- Deferred[IO, Throwable]
      processor = new SingletonProcessor(fiberRef, failureDeferred)
      result <- IO.race(block(processor), processor.failure).flatMap {
        case Left(result) => IO.pure(result)
        case _: Right[T, Nothing] => IO.raiseError(new RuntimeException("Impossible (returned Nothing)"))
      }.guarantee(processor.cancel)
    } yield result
  }
}
