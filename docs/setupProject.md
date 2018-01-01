# Setting up your project

These tests are written in Java.  To properly use this, we recommend that you setup a maven or gradle project.  If you are unsure how to do this, copy the `exampleUsage` folder and you can start from there.

## How do I setup a Java Project!?!

The easiest way is to copy paste.  [You can download an example project here](https://minhaskamal.github.io/DownGit/#/home?url=https://github.com/Kroger-Technology/Snow-Globe/tree/master/exampleUsage)  More information on the example project [can be found here.](https://github.com/Kroger-Technology/Snow-Globe/tree/master/exampleUsage)

## I got this Java thing.

If you are confident about setting up your own project, then you want to add in the snow-globe library as part
of your tests dependency.  The latest version is here:

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/com.kroger.oss/snow-globe/2.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.kroger.oss%22%20AND%20a%3A%22snow-globe%22)

The Snow-Globe library uses it's own set of asserts so we typically see that you will only need junit.  Below are example gradle and maven setups.

***Gradle (build.gradle)***
```
apply plugin: 'java'

repositories { jcenter() }

dependencies {
    // Here you are going to want to put in the latest version found here:
    // http://search.maven.org/#search%7Cga%7C1%7Ca%3A%20%22snow-globe%22%20g%3A%20%22com.kroger.oss%22
    testCompile 'junit:junit'
    testCompile 'com.kroger.oss:snow-globe:2.0-22-ga45ca69'
}

// This is optional to show test and Nginx logs in the std out/err (if wanted)
test {
    testLogging {
        showStandardStreams = true
    }
}
```

*** Maven (pom.xml) ***
```
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>nginx-tests</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>minimal-pom</name>
    <url>http://maven.apache.org</url>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>1.8</java.version>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.1</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.11</version>
            <scope>test</scope>
        </dependency>
        <!-- Here you are going to want to put in the latest version found here: -->
        <!-- http://search.maven.org/#search%7Cga%7C1%7Ca%3A%20%22snow-globe%22%20g%3A%20%22com.kroger.oss%22 -->
        <dependency>
            <groupId>com.kroger.oss</groupId>
            <artifactId>snow-globe</artifactId>
            <version>2.0-22-ga45ca69</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>

```

Once you have a project setup, click on the link below to configure snow-globe to execute your tests.

### [Next: Configure Snow-Globe](https://kroger-technology.github.io/Snow-Globe/configure)