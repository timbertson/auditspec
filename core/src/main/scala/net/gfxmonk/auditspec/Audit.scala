package net.gfxmonk.auditspec

import cats.effect.Resource
import cats.effect.concurrent.{MVar, MVar2}
import _root_.monix.eval.Task
import _root_.monix.execution.Scheduler
import _root_.monix.execution.schedulers.CanBlock
import _root_.monix.reactive.subjects.Var

import scala.collection.immutable.Queue

class Audit[T] private(state: MVar2[Task, List[T]], latest: Var[List[T]]) {
  private def update[R](modify: List[T] => (List[T], R)): Task[R] = {
    state.take.flatMap { current =>
      val (updated, result) = modify(current)
      for {
        // emit and then write back to state
        _ <- Task(latest := updated)
        _ <- state.put(updated)
      } yield result
    }.uncancelable
  }

  def get = state.read

  def record(interaction: T): Task[Unit] = update(list => (list.appended(interaction), ()))

  def reset: Task[List[T]] = update(original => (Nil, original))

  def waitUntil(predicate: List[T] => Boolean): Task[List[T]] = latest.filter(predicate).firstL

  def waitUntilEvent(predicate: T => Boolean): Task[List[T]] = waitUntil(list => list.exists(predicate))
}

object Audit {
  def apply[T] = Task.deferAction { scheduler =>
    val initial = List()
    val changes: Var[List[T]] = Var[List[T]](initial)(scheduler)
    MVar.of[Task, List[T]](initial).map(state => new Audit(state, changes))
  }

  // convenience constructor outside of Task
  def unsafe[T] = apply[T].runSyncUnsafe()(Scheduler.global, implicitly[CanBlock])

  def resource[T] = Resource.liftF(apply[T])

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