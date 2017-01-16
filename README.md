# Nginx Snow-Globe
How to simulate and test most any scenario when using NGINX as a reverse proxy in your own little world.

## Overview

This project was created to test out NGINX configuration.  We move beyon the "-t" option to validate the configuration.  This framework builds an isolated environment of upstream servers and then sends a request into the NGINX instance.  This "magic" is done by parsing the configuration, dynamically building an upstream file, and with the power of Docker Compose, simulating an actual flow through your NGINX configuration.


## How To Use Snow-Globe
To use this framework, here are the pre-requesites for your machine:
- Docker (1.11.0 or greater)
- Docker Compose (1.7.0 or greater)
- Java JDK (version 1.8.0 or greater)

#### Project Setup
You will need to do three things to use this framework:
1.  Setup a gradle or maven project with the libarary [here in artifactory](http://artifactory.kroger.com/artifactory/webapp/#/artifacts/browse/tree/General/kroger-dcp/com/kroger/dcp/nginxSnowGlobe/nginx-snow-globe) set.
2.  Configure `snow-globe.yaml`
3.  Write your tests.

The sections below will show you how to do this.

**Setup a gradle or maven Project**
You need to include in your test part of compilation, the latest jar from here: http://artifactory.kroger.com/artifactory/webapp/#/artifacts/browse/tree/General/kroger-dcp/com/kroger/dcp/nginxSnowGlobe/nginx-snow-globe

Below is an example snippet for gradle:
```
compileTest(group: 'com.kroger.dcp.nginxSnowGlobe', name: 'nginx-snow-globe', version: '1.0-1-gffe2f52')
```
Below is an example snipppet for maven:
```
<dependency>
    <groupId>com.kroger.dcp.nginxSnowGlobe</groupId>
    <artifactId>nginx-snow-globe</artifactId>
    <version>1.0-1-gffe2f52</version>
</dependency>
```

**Configure `snow-globe.yaml`**
The yaml file has several entries that need to be completed.  Below is the documenation on this:
```
nginx.volume.mounts:
# This is a list of files that you want to have mounted into your NGINX instance.  Below shows a single file
# that is being #mounted to "/etc/nginx/nginx.conf" inside of the container.  You can also specify
# directories to cover multiple files.
  - "src/nginx/nginx.conf:/etc/nginx/nginx.conf"
nginx.upstream.file.path:
# this is the location of the upstream file that will be created inside of the container.  Your
# configuration _MUST_ reference this file so it can be properly inserted.  You should not mount your
# own upstream file in the mounts section.  The framework will build it for your.
  "/etc/nginx/upstreams.conf"
nginx.container:
# The docker container to use to run your test.  This should be a working nginx container with the
# command "nginx" on the path.
  "docker.kroger.com/library/nginx:1.11.3"
nginx.url.port.mapping:
# A mapping of the url to the port on your NGINX configuration.  The first is a simple name to identify
# your mapping.  The first parameter "pattern" is a regular expression to match for this port.  The
# "port" specifies which port on your nginx instance to route.  Below shows the simple pattern to
# direct all https calls to 443 and all http: calls to 80 on the container.
  - https:
      pattern: "https:.*"
      port: 443
  - http:
      pattern: "http:.*"
      port: 80
upstream.fake.container:
# This is the name of the container for the fake upstream service.  The project has the ability to build
# the nodejs application that will reflect back the response.
  "docker.kroger.com/nginx/fake-upstream-service:1.0"
framework.log.output:
# If set to true, then each test will output the NGINX logs for each test.
  true
```

List of things to Do:

1.  Setup JIRA project to track internal tickets.
2.  Update README file to include up-to-date information on each of the entries.
3.  Add in unit tests for the tooling classes.
4.  Add in the applicable license.
5.  Add in a documentation header for each class and for each method.
6.  Add in integration tests for the following methods denoted in ResponseVerification.java
7.  Add in documentation for each type of integration test that was created in the README.md