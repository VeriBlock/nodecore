## Getting started

Compiling the VeriBlock packages requires the Java 8 JDK from Oracle.

### Building from the command line

The following commands are for Mac/Linux. For Windows, use the .bat variants

To perform a full build, run the following

    ./gradlew clean build
    

#### Open Source Tooling

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications.

YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![yourkit logo](https://www.yourkit.com/images/yklogo.png)

#### FAQ

1. IDEA can't see class VeriBlockMessages (class is too large). 
    Step 1 Open the menu item: «Help» → «Edit Custom Properties» 
    Step 2 Set the parameter: 
        idea.max.intellisense.filesize=999999 
        idea.max.content.load.filesize=200000
    Step 3 Restart IntelliJ IDEA.

