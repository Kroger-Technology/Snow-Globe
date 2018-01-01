<h1 align="center">Snow-Globe</h1>

An integration testing framework for Nginx that will test your configuration.  Every test involves verifying that a request will correctly flow through Nginx, to an upstream cluster and properly handles the response.

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/com.kroger.oss/snow-globe/2.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.kroger.oss%22%20AND%20a%3A%22snow-globe%22) [![Build Status Image](https://circleci.com/gh/Kroger-Technology/Snow-Globe.svg?style=shield&circe-token=bb34e5439f189eb33ad7591f59f768bd257aa0b8)](https://circleci.com/gh/Kroger-Technology/Snow-Globe) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


Snow-Globe completes an integration test by parsing the configuration, dynamically building an upstream file, and with Docker, it starts up Nginx with the configuration and verifies an actual request/response flow matches the test specifications.  Snow-Globe stubs the upstreams so that the entire environment can be fully controlled.


## Prerequisites
This project uses Java to run the tests and framework, and Docker to build the isolated enviornment for each test.

Below are the version requirements:

- Docker (1.12.0 or greater)
- Docker Compose (1.7.0 or greater)
- Java JDK (version 1.8.0 or greater)

## How to use Snow-Globe

Snow-Globe documentation is found on the github pages for the project: https://kroger-technology.github.io/Snow-Globe/

If you want to skip reading and just try it out.  Head over to our example project to run tests yourself: https://github.com/Kroger-Technology/Snow-Globe/tree/master/exampleUsage