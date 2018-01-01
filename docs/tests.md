
# Test Examples

Below are example tests and assertions that can be made for an Nginx configuration.

## Setup Pattern
Tests are setup by starting the Nginx environment.  You need to declare your upstream servers and then
start the Nginx configuration.

The code snipped below shows a common pattern of starting and stopping the nginx cluster for tests.  This
snippet comes from [src/test/java/com/kroger/snowGlobe/integration/tests/AppUrlTest](https://github.com/Kroger-Technology/Snow-Globe/blob/master/src/integration/java/com/kroger/oss/snowGlobe/integration/tests/AppUrlTest.java#L38)

```java
    public NginxRpBuilder nginxReverseProxy;
    public AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster");

    @Before
    public void setup() {
        nginxReverseProxy = NginxRpBuilder.runNginxWithUpstreams(loginUpstreamApp);
    }
```

Below are example test scenarios that include configuration and testing examples.  All of the examples below are used as 
integration tests for the project and pass for each build.

---

## Verifying the HTTP status code response to the client.

There are times where nginx or the upstream server set the HTTP response code and we can verify the response code whether
it is set in Nginx or the upstream server.

**Simple Example**

This is an example that verifies that the upstream server that sends a 200 will return back to the client with a 200.

Example Nginx code snippet (from [src/integrationTestNginxConfig/nginx.conf](https://github.com/Kroger-Technology/Snow-Globe/blob/master/src/integration/resources/nginx.conf#L23)):
```
   location /login {
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header host $host;
       proxy_pass  https://Login_Cluster/login-path;
   }
```

Example test code snippet (from [src/test/java/com/kroger/snowGlobe/integration/tests/StatusCodeTest.java](https://github.com/Kroger-Technology/Snow-Globe/blob/master/src/integration/java/com/kroger/oss/snowGlobe/integration/tests/StatusCodeTest.java#L46))
```java
    @Test
    public void should_return_200_for_login() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectResponseCode(200);
    }
```

---

## HTTP -> HTTPS redirect Example

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/StatusCodeTest.java`)

```java
    @Test
    public void should_return_301_http_to_https() {
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectResponseHeader("Location", "https://www.nginx-test.com/")
                .andExpectResponseCode(301);
    }
```

---

## Adding health checks to your requests

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/HealthCheckTest.java`)
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

---

## Verifying the call was routed to the correct upstream cluster

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/ClusterNameTest.java`)
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

---

## Verifying the call sent to the upstream app uses the correct path

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/AppPathTest.java`)
```java
    @Test
    public void should_route_login_request_to_login_path() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectAppPath("/login-path");
    }
```

---

## Verifying the call to the upstream application contains a specific header

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/RequestHeaderTest.java`)
```java
    @Test
    public void should_add_x_proto_header_to_login_request() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectRequestHeaderToApplicationMatching("x-forwarded-proto", "https")
                .andExpectRequestHeaderToApplicationMatching("host", "www.nginx-test.com");
    }
```

---

## Verifying the call to the upstream application has the url fields properly set.

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/AppUrlTest.java`)
```java
    @Test
    public void should_properly_pass_url_fields() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectAppUrl("https://www.nginx-test.com/login-path");
    }
```

---

## Verifying the response headers

When Nginx routes a call using the `proxy_pass` directive, this test can verify that the response contains specific 
headers.  One interesting thing to note is that you can configure your upstream bounce servers to respond with specific
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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/ResponseHeaderTest.java`)
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
    
    @Test
    public void should_have_matching_response_header() {
        make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
                .andExpectResponseHeaderMatches("internal-secret-key", "[0-9]+");
    }

```

---

## Verifying the call is made to a specific upstream server

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/ClusterNumberTest.java`)
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

---

## Verifying the call made to an upstream server contains a query param

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/QueryParamTest.java`)
```java
    @Test
    public void should_convert_path_to_query_param() {
        make(getRequest("https://www.nginx-test.com/search/milk").to(nginxReverseProxy))
                .andExpectToHaveQueryParam("q", "milk");
    }
```

---

## Verifying the response matches a file or string

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

Example test code snippet (from `src/test/java/com/kroger/snowGlobe/integration/tests/FileResponseTest.java`)
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