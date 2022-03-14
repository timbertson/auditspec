package net.gfxmonk.auditspec

import cats.effect.kernel.{Concurrent, Deferred, Ref, Resource}
import cats.implicits._

import scala.collection.immutable.Queue

private sealed trait WatchResponse
private case object KeepWatching extends WatchResponse
private case object StopWatching extends WatchResponse

// waiters is a list of functions. When one return an F[Unit], it means its watch
// is complete (and we want to trigger the effect)
private case class State[F[_], T](value: List[T], waiters: List[List[T] => Option[F[Unit]]])

class Audit[F[_], T] private(state: Ref[F, State[F, T]])(implicit io: Concurrent[F]) {
  private def update[R](modify: List[T] => (List[T], R)): F[R] = {
    state.modify { (lastState: State[F, T]) =>
      val (updated, result) = modify(lastState.value)
      val (remainingWaiters, triggerWaiters) = lastState.waiters.partitionMap { fn =>
        fn(updated).toRight(fn)
      }
      val nextState = State(updated, remainingWaiters)
      val action = triggerWaiters.sequence_.as(result)
      (nextState, action)
    }.flatMap(identity[F[R]])
  }

  def get = state.get.map(_.value)

  def record(interaction: T): F[Unit] = update(list => (list.appended(interaction), ()))

  def reset: F[List[T]] = update(original => (Nil, original))

  def waitUntil(predicate: List[T] => Boolean): F[List[T]] = {
    for {
      d <- Deferred[F, List[T]]
      action <- state.modify { currentState =>
        val maybeComplete: List[T] => Option[F[Unit]] = value => if (predicate(value)) {
          Some(d.complete(value).void)
        } else {
          None
        }

        maybeComplete(currentState.value) match {
          case Some(action) => (currentState, action)
          case None => (currentState.copy(waiters = maybeComplete :: currentState.waiters), io.unit)
        }
      }
      _ <- action
      result <- d.get
    } yield result
}

  def waitUntilEvent(predicate: T => Boolean): F[List[T]] = waitUntil(list => list.exists(predicate))
}

object Audit {
  def apply[F[_], T](implicit io: Concurrent[F]) = {
    Ref[F].of[State[F, T]](State[F, T](Nil, Nil)).map { ref => new Audit[F, T](ref) }
  }

  def resource[F[_], T](implicit io: Concurrent[F]) = Resource.eval(apply[F, T])

  private def partitionAcc[T](acc: Queue[List[T]], expected: List[List[T]], events: List[T]): List[List[T]] = {
    (expected, events) match {
      case (_, Nil) => acc.toList
      case (Nil, excess) => acc.appended(excess).toList
      case (head :: tail, events) => {
        val (headEvents, tailEvents) = events.splitAt(head.length)
        partitionAcc(acc.appended(headEvents), tail, tailEvents)
      }
    }
  }

  private def sortPartitioned[T](list: List[List[T]])(implicit ordering: Ordering[T]) = list.map(chunk => chunk.sorted)

  /** Utility function for partially ordered results.
    * For example, you might have an interaction where you know that A and B events will happen concurrently, but they'll both occur before C and D.
    * Because A/B and C/D are concurrent, you may get any of:
    *  - List(A, B, C, D)
    *  - List(B, A, C, D)
    *  - List(A, B, D, C)
    *  - List(B, A, C, D)
    *
    * But that's awkward to match against, and you don't want to just discard ordering.
    *
    * In this case you could use:
    * val (actual, expected) = partitionAndSort(eventList, List(List(A, B), List(C, D)))
    * assert(actual == expected)
    *
    * Each chunk will be sorted (to remove concurrent ordering issues), but the order of chunks will be preserved.
    */

  def partitionAndSort[T](events: List[T], expected: List[List[T]])(implicit ordering: Ordering[T]): (List[List[T]], List[List[T]]) = {
    val partitionedEvents = partitionAcc(Queue.empty, expected, events)
    (sortPartitioned(partitionedEvents), sortPartitioned(expected))
  }
}