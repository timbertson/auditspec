<img src="http://gfxmonk.net/dist/status/project/auditspec.png">

# Auditspec

Add to build.sbt:

```
libraryDependencies += "net.gfxmonk" %% "auditspec" % "VERSION" // check github tags for latest version
```

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

That's right, it's basically just a `ListBuffer`. The idea is that when you need to test certain side effects, you inject test stubs which just record events in this list. Then at the end of the test, you assert which effects have happened. You're not limited to `String` events, you can (and should) make your own event types to enable better filtering/etc.

Why not just use a `ListBuffer`? You're welcome to, but `auditspec` has a couple of extra features:

 - it's built on cats-effect IO, to represent side effects purely
 - it's thread-safe
 - it comes with extra `waitFor( ... )` methods so you can spawn some actions and then wait until certain interaction(s) have occurred
 - `reset()` for complex tests, where you might want to break up the interaction log into a sequence of chunks to be verified individually

And... that's about it.

If IO doesn't work for your use case, feel free to contribute alternative modules, or simply copy/paste what you need. It's really a [very small](core/src/main/scala/net/gfxmonk/auditspec/Audit.scala) library.
