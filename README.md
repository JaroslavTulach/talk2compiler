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

