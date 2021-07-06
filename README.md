# Auditspec

Auditspec is a ridiculously simple library for helping you test side effects in code. Usage is dirt simple, as exemplified by its own test case:

```scala
test("record / reset / get") {
  Audit.resource[String].use { audit =>
    for {
      _ <- audit.record("a")
      _ <- audit.record("b")
      _ <- audit.record("c")
      initialEvents <- audit.reset
      _ <- audit.record("d")
      _ <- audit.record("e")
      _ <- audit.record("f")
      finalEvents <- audit.get
    } yield {
      expect.all(
        initialEvents == List("a", "b", "c"),
        finalEvents == List("d", "e", "f"))
    }
  }
}
```

That's right, it's basically just a `ListBuffer`. But it has some handy features:

 - it's built on monix Task, to represent side effects purely
 - it's thread-safe
 - it comes with extra `waitFor( ... )` methods so you can spawn some actions and then wait until certain interaction(s) have occurred
 - `reset()` for complex tests, where you might want to break up the interaction log into a sequence of chunks to be verified individually

And... that's about it.

If monix' Task doesn't work for your use case, feel free to contribute alternative modules, or simply copy/paste what you need. It's really a [very small](core/src/main/scala/net/gfxmonk/auditspec/Audit.scala) library.
