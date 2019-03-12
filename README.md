# Talk to (Graal) Compiler (on JDK11)

Sample project showing how to control [Graal Compiler](http://graalvm.org)
via Truffle API.

## Pre requirements

- Linux or Mac OS
- [Maven](https://maven.apache.org)
- [JDK11](https://jdk.java.net/11/)

## Setup

- Clone this repository
```
git clone https://github.com/jaroslavtulach/talk2compiler
```

- Move to the newly cloned directory
```
cd talk2compiler
```

- Package and run the project using Maven
```
JAVA_HOME=/jdk-11 mvn package exec:exec
```

## Working with the sources

The project stands in its `master` state as a platform 
ready for your experimentations. Use it to get the
[Graal JIT compiler](http://graalvm.org)
under your control. Use it to get the best from the dynamic just in time
compiler surrouding your code. Just enclose your code into the 
`Main.execute` method and you'll be in direct contact to the assembly -
code in Java, but directly influence the native code. That is what 
we are aiming at!

