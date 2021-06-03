package net.gfxmonk.auditspec.example

import net.gfxmonk.auditspec.{Audit, TestSchedulerSuite}
//import cats.implicits._
import scala.concurrent.duration._
import _root_.monix.eval.Task


object ProcessorSpec extends TestSchedulerSuite {
  private val resource = Audit.resource[String]
}