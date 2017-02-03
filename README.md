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


# Examples Test Scenarios

Below are example test scenarios that include configuration and testing examples.  All of the examples below are used as 
integration tests for the project and pass for each build.

## Verifying the HTTP status code response to the client.

There are times where nginx or the upstream server set the HTTP response code and we can verify the response code whether
it is set in Nginx or the upstream server.

**Simple Example**

This is an example that verifies that the upstream server that sends a 200 will return back to the client with a 200.

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login;
   }
```

Example test code snippet (from `src/test/java/com.kroger.snowGlobe.integration.tests.StatusCodeTest`)
```
    @Test
    public void should_return_200_for_login() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectResponseCode(200);
    }
```

---
***HTTP -> HTTPS redirect Example***

This is an example that an http call will return a HTTP `301` and have a location header to use HTTPS

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
 server {
     listen *:80;
     server_name
        www.nginx-test.com;

        location / {
            return 301 https://$host$request_uri;
        }
  }
```

Example test code snippet (from `src/test/java/com.kroger.snowGlobe.integration.tests.StatusCodeTest`)
```
    @Test
    public void should_return_301_http_to_https() {
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectResponseHeader("Location", "https://www.nginx-test.com/")
                .andExpectResponseCode(301);
    }
```
