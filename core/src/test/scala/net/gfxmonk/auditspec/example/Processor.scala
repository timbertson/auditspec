package net.gfxmonk.auditspec.example

import monix.eval.Task
import monix.reactive.Observable

class Processor[Message, Key](inputs: Observable[Message], process: Message => Task[Unit], getKey: Message => Key) {

}

object Processor {
}
