# Talk to (Graal) Compiler (on JDK11)

Sample project showing how to control [Graal Compiler](http://graalvm.org)
via Truffle API.

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
$ JAVA_HOME=/jdk-11 mvn test -Digv.args=-Dgraal.Dump=:1
```
A tree of graphs representing *Truffle::Main* shall appear in the
[IGV](https://www.graalvm.org/docs/graalvm-as-a-platform/implement-language/#igv).
The most interesting phase is *Graal Graphs/Before phase Lowering* - it
contains all the Graal optimizations, yet the vertexes still resemble bytecode
instructions and are OS architecture neutral.
