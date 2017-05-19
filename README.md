# Snow-Globe

A end to end testing framework that will simulate your upstream servers and help verify that Nginx properly routes all 
of your calls.  It verifies that your configuration decorates the request to the upstream servers and the responses back
work as expected.

We move beyond the "-t" option to validate the configuration.  This framework builds an isolated environment of upstream servers and then sends a request into the NGINX instance.  This "magic" is done by parsing the configuration, dynamically building an upstream file, and with the power of Docker Compose, simulating an actual flow through your NGINX configuration.

## How To Use Snow-Globe
To use this framework, here are the pre-requisites for your machine:
- Docker (1.11.0 or greater)
- Docker Compose (1.7.0 or greater)
- Java JDK (version 1.8.0 or greater)

## Project Setup
You will need to do four things to use this framework:
1.  Setup a gradle or maven project with the library set.
2.  Make sure your upstreams are defined in a separate file (or better yet directory).
3.  Configure `snow-globe.yaml`
4.  Write your tests.tests.

The sections below will show you how to do this.

## Step 1: Setup your project
These tests.tests are written in Java.  To properly use this, we recommend that you setup a maven or gradle project.  If you are unsure how to do this, copy the `exampleUsage` folder and you can start from there.

Below is an example snippet for gradle:

```groovy
compileTest(group: 'com.kroger.dcp.snowGlobe', name: 'snowGlobe', version: '1.0-1-gffe2f52')
```
Below is an example snippet for maven:

```xml
<dependency>
    <groupId>com.kroger.dcp.snowGlobe</groupId>
    <artifactId>snowGlobe</artifactId>
    <version>1.0-1-gffe2f52</version>
</dependency>
```

## Step 2: Make sure your Nginx Configuration is setup to be used by the tests.tests.

SnowGlobe will crawl your configuration for each test and build the temporary upstreams.  So to be able to do that, it
 needs to have the upstreams define in a separate file that are _not_ included in the configuration.  We recommend that
 you have your upstreams in a separate directory.  That makes it more simple to include/exclude files for the tests.tests.

You can see an example Nginx setup in the `exampleUsage` directory.

## Step 3: Setup the SnowGlobe Yaml file

**Configure `snow-globe.yaml`**
The yaml file has several entries that need to be completed.  Below is the documentation on this.

The first part of the manual is how we map your configuration files into the docker container.  No matter the method, you
will be transfering your configuration to your destination environment.  We use `nginx.volume.mounts` to map your local
files into the path on the docker image.  Here is an example:

```yaml
nginx.volume.mounts:
    defaults:
      - "src/nginx/nginx.conf:/etc/nginx/nginx.conf"
```
The `nginx.volume.mounts` is an array of mappings.  You can mount a single file, a directory, or a wildcard.  One
thing to note is that you can't map your upstream file.  SnowGlobe is going to build one for you and put it in.  Here
is an example mounts with different mapping techniques.

```yaml
nginx.volume.mounts:
  default:
    - "src/nginx/*:/webApps/nginx/conf/"
    - "src/nginx/html:/webApps/nginx/conf/html"
    - "src/nginx/env/prod/*:/webApps/nginx/conf/env/prod/"
    - "src/nginx/env/prod/certs/:/webApps/nginx/conf/env/prod/certs/"
    - "src/nginx/env/prod/locations/:/webApps/nginx/conf/env/prod/locations/"
    - "src/nginx/env/prod/servers/:/webApps/nginx/conf/env/prod/servers/"
    - "src/scripts/startTest.sh:/startTest.sh"
```

**NOTE:**  There is the `default` setting where you can have different mappings.  We do this to allow you to define
a mapping for your environments.  You can map different files for dev/test/prod.  If you are just starting out, try
just the "default" mapping.

Next up, you need to help SnowGlobe tell us where you put you upstream file(s).  This is done because SnowGlobe will 
generate one for you and drop it in for you.  This is the file location on the deployed configuration, not the path
for the source code.  Here is an example:

```yaml
nginx.upstream.file.path:
  "/etc/nginx/upstreams.conf"
```

After that, we need to know what container you are using to run Nginx.

```yaml
nginx.container:
  "docker.kroger.com/library/nginx:1.11.3"
```

This next part is the port mapping for traffic.  Most people send HTTPS -> 443 and HTTP -> 80.   But we have had
cases where this is not true.  This section allows you to define the traffic and how you want to map it.  You can
define multiple patterns for the url and which port you route traffic to.

```yaml
nginx.url.port.mapping:
  - https:
      pattern: "https:.*"
      port: 443
  - http:
      pattern: "http:.*"
      port: 80
```

This next field is the docker container to use for the fake upstream service.

```yaml
upstream.fake.container:
  "docker.kroger.com/nginx/fake-upstream-service:1.0"
```

This next field defines how to start nginx.   You may have a custom script that you
use and this is where you run that.  This can have environment specific startup commands
so the `default` field is needed for the base run command.

```yaml
nginx.start.command:
  default: ["nginx", "-g", "'daemon off;'"]
```

The next two fields define the "base" directory for you configuration.  It is popular to
use relative paths for includes and such.  This gives SnowGlobe advice on how to interpret 
includes using a relative path:

```yaml
nginx.source.base.directory: "src/integrationTestNginxConfig/"
nginx.deploy.base.directory: "/etc/nginx/"
```

This next field tells SnowGlobe where to search when inspecting the configuration.  Many times, this is
just the nginx.conf or base file that is used for the configuration.  If you deployment is more than just copying
a directory, you will need to tell SnowGlobe where all of the moved files are.  

```yaml
nginx.env.config.files:
  default:
    - "/src/integrationTestNginxConfig/nginx.conf"
```

The next field indicates where to pull the fake upstream container:
```yaml
upstream.fake.container:[TBD]
```

This field will when set to true will output all logs from the framework and docker containers:
```yaml
snowglobe.log.output:  false
```
This field will preserve the compose and upstream files for help in investigating a problem:
 ```yaml
snowglobe.preserve.temp.files: false
```
This field will disable extra logging that is mostly not needed:
```yaml
snowglobe.disable.commons.logging: true
```
This last field will add upstream zones if you are using Nginx Plus. 
```yaml
nginx.define.upstream.zones: true
```

## Step 4: Write the tests.tests

Writing the tests.tests is one of the easy parts and also one of the most important.  Starting up the Docker compose 
environment can be slow (.5 - 3 seconds) and so we recommend that you do that you combine tests.tests for similar scenarios.

### Test Setup

Tests are setup by starting the Nginx environment.  You need to declare your upstream servers and then
start the nginx configuration.

The code snipped below shows a common pattern of starting and stopping the nginx cluster for tests.tests.  This
snippet comes from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/AppUrlTest`

```java
    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(loginUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }
```

This is done not inside a function but at the class level.  The `@BeforeClass` method is run once before
the tests.tests in the class.  You can see that it starts the Nginx Configuration with the login upstream cluster defined. You
don't need to define every upstream, just the one that you are interested in testing. You can define multiple upstreams
to set their behavior to test non-200 scenarios.  

### Examples Test Scenarios

Below are example test scenarios that include configuration and testing examples.  All of the examples below are used as 
integration tests.tests for the project and pass for each build.

#### Verifying the HTTP status code response to the client.

There are times where nginx or the upstream server set the HTTP response code and we can verify the response code whether
it is set in Nginx or the upstream server.

**Simple Example**

This is an example that verifies that the upstream server that sends a 200 will return back to the client with a 200.

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login-path;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/StatusCodeTest.java`)
```java
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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/StatusCodeTest.java`)

```java
    @Test
    public void should_return_301_http_to_https() {
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectResponseHeader("Location", "https://www.nginx-test.com/")
                .andExpectResponseCode(301);
    }
```


#### Adding health checks to your requests

***Successful Health Check***

This is an example that a health check is defined and called in addition to your base call. If you do not define a health check endpoint and call expectSuccessfulHealthCheck you will receive a fail() message

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
 server {
     listen *:80;
     server_name
        www.nginx-test.com;

        location / {
                   proxy_set_header host $host;
                   proxy_pass  http://Content_Cluster;
               }
        location /healthcheck {
                   return 200;
        }
  }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/HealthCheckTest.java`)
```java
    @Test
    public void should_have_successful_health_check() {
        make(getRequest("https://www.nginx-test.com")
                .withHealthCheck("/healthcheck")
                .to(nginxReverseProxy))
                .andExpectClusterName("Content_Cluster")
                .expectSuccessfulHealthCheck();
    }
```
#### Verifying the call was routed to the correct upstream cluster

When Nginx routes a call using the `proxy_pass` directive, this test can verify that it was sent to the correct cluster.

**Simple Example**

This verifies that the login calls are routed to the login cluster.  We have another test that verifies that it is 
routed to the item cluster

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login-path;
   }
   
   location /item {
      proxy_set_header host $host;
      proxy_pass  http://Item_Cluster/item;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/ClusterNameTest.java`)
```java
    @Test
    public void should_route_login_request_to_login_cluster() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectClusterName("Login_Cluster");
    }
    
    @Test
    public void should_route_item_request_to_item_cluster() {
        make(getRequest("https://www.nginx-test.com/item").to(nginxReverseProxy))
                .andExpectClusterName("Item_Cluster");
    }
```


#### Verifying the call sent to the upstream app uses the correct path

When Nginx routes a call using the `proxy_pass` directive, this test can verify that it was sent to the upstream app with
the correct path.

**Simple Example**

This verifies that the login calls are routed with the upstream path of "/login-path".

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login-path;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/AppPathTest.java`)
```java
    @Test
    public void should_route_login_request_to_login_path() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectAppPath("/login-path");
    }
```

#### Verifying the call to the upstream application contains a specific header

When Nginx routes a call using the `proxy_pass` directive, this test can verify that it was sent to the upstream app with
the a specific header and the value matches a regex pattern.  This also verifies that the host header field is properly 
set using a dynamic variable.

**Simple Example**

This verifies that the login calls are routed with the "x-forwarded-proto" header field sent with a value of "https"

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login-path;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/RequestHeaderTest.java`)
```java
    @Test
    public void should_add_x_proto_header_to_login_request() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectRequestHeaderToApplicationMatching("x-forwarded-proto", "https")
                .andExpectRequestHeaderToApplicationMatching("host", "www.nginx-test.com");
    }
```

#### Verifying the call to the upstream application has the url fields properly set.

When Nginx routes a call using the `proxy_pass` directive, this test can verify that it was sent to the upstream app with
the host, protocol, and url field.

**Simple Example**

This verifies that the login calls are routed with the with the host, protocol and url fields properly set

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login-path;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/AppUrlTest.java`)
```java
    @Test
    public void should_properly_pass_url_fields() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectAppUrl("https://www.nginx-test.com/login-path");
    }
```

#### Verifying the response headers

When Nginx routes a call using the `proxy_pass` directive, this test can verify that the response contains specific 
headers.  One interesting thing to note is that you can configure your fake upstream servers to respond with specific
headers.  Another test is that you might want Nginx to remove a response header.

**Simple Example**

This verifies that the that the response header contains specific fields and values that can be populated by the upstream
response or from the nginx configuration.  The second test verifies that response does not contain a response header 
that was generated by the upstream server, but the `proxy_hide_header` directive suppresses it.

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /checkout {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       add_header rp-response-header true;
       proxy_hide_header internal-secret-key;
       proxy_pass  https://Cart_Cluster/cart/checkout;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/ResponseHeaderTest.java`)
```java
    // A custom upstream app can be created that defines response headers.
    public static AppServiceCluster cartUpstreamApp = makeHttpsWebService("Cart_Cluster", 1)
            .withResponseHeader("got-cart", "success")
            .withResponseHeader("internal-secret-key", "42");
    //...
                
    @Test
    public void should_return_response_headers() {
        make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
            .andExpectResponseHeader("got-cart", "success") // this comes from the fake upstream
            .andExpectResponseHeader("rp-response-header", "true"); // this comes from the Nginx Configuration
    }
    
    @Test
    public void should_not_return_secret_header() {
        make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
                .andExpectMissingResponseHeader("internal-secret_key");
    }

```


#### Verifying the call is made to a specific upstream server

When Nginx routes a call using the `proxy_pass` directive, this test can verify that the request is sent to a specific 
upstream instance.  This is useful to verify the weighting, and load distribution configuration.

**Simple Example**

This verifies that the calls are made in a round robin distribution.

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
    location /login {
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header host $host;
        proxy_pass  https://Login_Cluster/login-path;
    }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/ClusterNumberTest.java`)
```java
    // A custom upstream app can be created that defines response headers.
    public static AppServiceCluster cartUpstreamApp = makeHttpsWebService("Cart_Cluster", 1)
            .withResponseHeader("got-cart", "success");
            
    //... 
    
    @Test
    public void should_round_robin_each_request_to_each_upstream_instance() {
        range(0,10).forEach(clusterNumber ->
                make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectClusterNumber(clusterNumber));
    }
```


#### Verifying the call made to an upstream server contains a query param

When Nginx routes a call using the `proxy_pass` directive, this test can verify that the request is sent to an upstream
service has a specified query param set.

**Simple Example**

This verifies that a path variable from a regex expression is converted to a query parameter for an upstream service.

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location ~* /search/(.*) {
        proxy_pass http://Search_Cluster/search?q=$1;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/QueryParamTest.java`)
```java
    @Test
    public void should_convert_path_to_query_param() {
        make(getRequest("https://www.nginx-test.com/search/milk").to(nginxReverseProxy))
                .andExpectToHaveQueryParam("q", "milk");
    }
```


#### Verifying the response matches a file or string

When Nginx routes a call using the `proxy_pass` directive, this test can verify that response matches a string value or 
the contents of a file.

**Simple Example**

This verifies that `/body` matches the file in `src/integrationTestNginxConfig/static/static.html` and the string
contents

Example Nginx code snippet (from `src/integrationTestNginxConfig/nginx.conf`):
```
   location /body {
        try_files /static.html =404;
   }
```

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests.tests/FileResponseTest.java`)
```java
   @Test
   public void should_return_response_headers() {
       make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
               .andExpectResponseHeader("got-cart", "success")
               .andExpectResponseHeader("rp-response-header", "true");
   }
   
   @Test
   public void should_not_return_secret_header() {
       make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
               .andExpectMissingResponseHeader("internal-secret_key");
   }
```