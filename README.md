# Talk to (Graal) Compiler (on JDK11)

Sample project showing how to control [Graal Compiler](http://graalvm.org)
via Truffle API.

[![Build Status](https://travis-ci.org/JaroslavTulach/talk2compiler.svg?branch=master)](https://travis-ci.org/JaroslavTulach/talk2compiler)

## Pre requirements

- Linux or Mac OS
- [Maven](https://maven.apache.org)
- [JDK11](https://jdk.java.net/11/)

## Setup

```
$ git clone https://github.com/jaroslavtulach/talk2compiler
$ cd talk2compiler
JAVA_HOME=/jdk-11 mvn package exec:exec
```

## Working with the sources

The project stands in its `master` state as a platform 
ready for your experimentations. Use it to get the
[Graal JIT compiler](http://graalvm.org)
under your control. Use it to get the best from the dynamic just in time
compiler surrouding your code. Just enclose your code into the 
`Main.execute` method and you'll be in direct contact with the assembly -
code in Java, but directly influence the native code. That is what 
we are aiming at!

## Look at the Compiler

Download [GraalVM EE](http://graalvm.org) and launch
[Ideal Graph Visualizer](https://www.graalvm.org/docs/graalvm-as-a-platform/implement-language/#igv).
It starts listening on port 4445 and is ready to accept graphs showing
progress of Graal compilations. Run the tests to dump the graphs:
```
$ JAVA_HOME=/jdk-11 mvn test -Pigv
```
The `igv` profile passes additional arguments to the JVM:
`-Dgraal.Dump=:1 -Dgraal.PrintGraph=Network`.
A tree of graphs representing *Truffle::Main* shall appear in the
[IGV](https://www.graalvm.org/docs/graalvm-as-a-platform/implement-language/#igv).
The most interesting phase is *Graal Graphs/Before phase Lowering* - it
contains all the Graal optimizations, yet the vertexes still resemble bytecode
instructions and are OS architecture neutral.

## Exploring the Compilations

Try to add field into `Main` class called `warmWelcome`. Use different message
format when it is `true` and different when it is `false`. When you look at
the graph you shall see the load of the field value and `if` vertex.

Try to make the field `final`. The load and `if` disappears. Remove the `final`
modifier and annotate the field as `@CompilationFinal`. The result is the same.
Modify the value of the field (in the `Main.execute` method), but don't forget
to call `CompilerDirectives.transferToInterpreterAndInvalidate()` to tell the
compiler to recompile.

Rather than using these primitive operations, consider using profiles like
`ConditionProfile.createBinaryProfile()`. Profiles are built with the above
primitives, yet they are easier to use.

## Nodes and DSL

Create simple [AST to process array](https://github.com/JaroslavTulach/talk2compiler/commit/f316b428d5474a60b6eec760f2d54c67b7d397f1)
of numbers. Send the graph to IGV and see how the partial evaluation reduced
it to three load and two plus instructions. Change the example to use `Object[]`.
Rewrite `Plus.compute` to support not only `int`, but also `double` and/or any object.
Observe the gigantic IGV graph. Use compiler directives, profiles & etc. to
optimize the graph again. After realizing that it is too complex, use
the [DSL specialization](https://github.com/JaroslavTulach/talk2compiler/commit/af9d269aafc1c3fb8d82f0a3db6437bedbcf40a6)
annotation processors to do the hard work for you.
