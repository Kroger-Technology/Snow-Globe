# Example Project For Snow-Globe

This provides an example project that you can use to test how snow-globe actually works.

You can [download the project directly to your machine to try it out!](https://minhaskamal.github.io/DownGit/#/home?url=https://github.com/Kroger-Technology/Snow-Globe/tree/master/exampleUsage)

##Useful commands:

To understand if you system has everything it needs:
```
./checkDependencies.sh
```

To Run the tests, just run the command:

```
./gradlew test
```

To force your tests to re-run, you can use the command:

```
./gradlew clean test
```

##Project contents

- `src/nginx`: All of the nginx configuration files that are being tested.
- `src/test/java/org/test/ExampleTest.java`: The test class that is used.
- `build.gradle`: A build tool configuration file to run the tests.
- `snow-globe.yaml`: The configuration file for the snow-globe framework.