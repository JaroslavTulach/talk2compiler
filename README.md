# Talk to (Graal) Compiler (on JDK11+)

Sample project showing how to control [Graal Compiler](http://graalvm.org)
via Truffle API.

[![Talk2Compiler](https://github.com/JaroslavTulach/talk2compiler/actions/workflows/maven.yml/badge.svg)](https://github.com/JaroslavTulach/talk2compiler/actions/workflows/maven.yml)
## Pre requirements

- Linux, Mac OS X, or Windows
- [Maven](https://maven.apache.org)
- [JDK11](https://jdk.java.net/11/) or [JDK17](https://jdk.java.net/17/)

## Setup

```
$ git clone https://github.com/jaroslavtulach/talk2compiler
$ cd talk2compiler
$ JAVA_HOME=/jdk-11 mvn package exec:exec -Dexec.appArgs=Truffle
```

## Working with the sources

The project stands in its `master` state as a platform 
ready for your experimentations. Use it to get the
[Graal JIT compiler](http://graalvm.org)
under your control. Use it to get the best from the dynamic just in time
compiler surrouding your code. Just enclose your code into the 
`Main.execute` method and you'll be in direct contact with the *assembly* -
code in Java, but directly *influence the native code*. That is what
we are aiming at!

## Look at the Compiler

Download [GraalVM EE](http://graalvm.org) and launch
[Ideal Graph Visualizer](https://docs.oracle.com/en/graalvm/enterprise/21/docs/tools/igv/).
It starts listening on port 4445 and is ready to accept graphs showing
progress of Graal compilations. Either run the program with `-Pigv`
```
$ JAVA_HOME=/jdk-11 mvn -Pigv process-classes exec:exec -Dexec.appArgs=Truffle
```
or run the tests to dump the execution graphs
```
$ JAVA_HOME=/jdk-11 mvn -Pigv test
```
The `igv` profile passes additional arguments to the JVM:
`-Dgraal.Dump=:1 -Dgraal.PrintGraph=Network`.
A tree of graphs representing *Truffle::Main* shall appear in the
[IGV](https://docs.oracle.com/en/graalvm/enterprise/21/docs/tools/igv/).
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

## Speed a Graph Algorithm up

The [BooleanNetwork](https://github.com/JaroslavTulach/talk2compiler/compare/BooleanNetwork) branch contains
a graph with states representing spreading of [tumor in a brain](https://github.com/JaroslavTulach/talk2compiler/blob/baafa0821eb81d6330e946d309669f6dede088e5/src/main/java/org/apidesign/demo/talk2compiler/bn/TumorCellPathway.java).
Let's search the graph for a state that matches [certain pattern](https://github.com/JaroslavTulach/talk2compiler/compare/BooleanNetwork#diff-699f4d29c2fc54c8baab4e1a2db5fead1d8e2b24aeca5fd7c02f983c0426676bR11) - `null`s are ignored, `true` and `false` values
must match. The [algorithm counts](https://github.com/JaroslavTulach/talk2compiler/compare/BooleanNetwork#diff-699f4d29c2fc54c8baab4e1a2db5fead1d8e2b24aeca5fd7c02f983c0426676bR88) how many nodes in the graph match.

The **algorithm** can run in _regular HotSpot mode_ as well as in _Truffle one_ [entered via CallTarget](https://github.com/JaroslavTulach/talk2compiler/compare/BooleanNetwork#diff-699f4d29c2fc54c8baab4e1a2db5fead1d8e2b24aeca5fd7c02f983c0426676bR26). When in _Truffle_ mode,
one can apply additional hints - namely `@CompilerDirectives.CompilationFinal(dimensions = 1)`, `@ExplodeLoop` and `CompilerAsserts.partialEvaluationConstant` -
to speed the execution up by expanding the [match loop](https://github.com/JaroslavTulach/talk2compiler/compare/BooleanNetwork#diff-699f4d29c2fc54c8baab4e1a2db5fead1d8e2b24aeca5fd7c02f983c0426676bR106) and eliminating the `null` checks.

## Build a Polymorphic Cache

The `@ExplodeLoop` annotation can be used to control the amount of generated code. Use it to build a [polymorphic cache](https://github.com/JaroslavTulach/talk2compiler/compare/PolymorphicCache) a _phone book_ mapping between names and numbers. Control the size of the cache (e.g. generated code) fallback to regular (slow) lookup in a `HashMap`.

## Truffle Nodes

Create simple [AST to process array](https://github.com/JaroslavTulach/talk2compiler/commit/f316b428d5474a60b6eec760f2d54c67b7d397f1)
of numbers. Send the graph to IGV and see how the partial evaluation reduced
it to three load and two plus instructions. Change the example to use `Object[]`.
Rewrite `Plus.compute` to support not only `int`, but also `double` and/or any object.
Observe the gigantic IGV graph. Use compiler directives, profiles & etc. to
optimize the graph again. After realizing that it is too complex, use
the Truffle DSL.

## Truffle DSL

Use [DSL specializations](https://github.com/JaroslavTulach/talk2compiler/commit/7f4f99940b9598bb745f71eeda9b9ac0f650a747)
to do the hard work for you. Remove even more boilerplate code with
[Truffle DSL type system](https://github.com/JaroslavTulach/talk2compiler/commit/33af44aa21466e9b5bcc61c163aa92b59b9ec2d4).
Follow the remaining commits, commit-by-commit, of the [Truffle DSL](https://github.com/JaroslavTulach/talk2compiler/compare/2022/dsl-typesystem)
branch to learn more.

## Control Flow

In [this branch](https://github.com/JaroslavTulach/talk2compiler/compare/2022/dsl-typesystem...2022/statements-and-expressions), which
can be followed commit-by-commit, we implement simple infrastructure around [expressions and statements](https://en.wikipedia.org/wiki/Statement_(computer_science%29#Expressions)) and
few control flow constructs to demonstrate how control flow can be implemented in Truffle based AST interpreters.

## Abstractions & Truffle

In [this branch](https://github.com/JaroslavTulach/talk2compiler/compare/2022/dsl-typesystem...2022/truffle-libs), which
can be followed commit-by-commit, we explore common patterns to build internal abstractions in Truffle based languages.
One of the patterns with special support from Truffle DSL are Truffle libraries, which are also used for implementing
the interoperability protocol between different Truffle languages.

## GraalVM contribution opportunities

* [Radixsort for R interpreter](https://github.com/oracle/fastr/issues/200)
* [Jackpot rule](https://netbeans.apache.org/jackpot/HintsFileFormat.html) integrated in [Truffle API](https://github.com/oracle/graal/blob/2fa61d7e952fc65f78e6d15674b9ed4afab58322/truffle/src/com.oracle.truffle.api/src/META-INF/upgrade/SuppressWarnings.hint) (runs as a part of `mx jackpot` command) that checks for common Truffle API usage issues (e.g., `@Child` field assignment outside of constructor without a call to `insert`).
* Pick your favourite Python package, run its test suite on CPython and GraalPython, if there are failures
specific to GraalPython, try to fix some of them. If it turns out to be too hard, report your findings
in a GitHub issue.
