# Upstream Bounce App

This was created to send back specific http codes with the request being set in the response body.


This is created as part of the [Snow Globe Project](https://github.com/Kroger-Technology/Snow-Globe)

### Using the Container

The container has several environment variables that are used to properly startup:
- `INSTANCE_NUMBER`: Used to identify a specific instance if the testing environment uses more than one.
- `CLUSTER_NAME`: The name of the upstream cluster associated with this container.  This is used with the number to create a unique identifier.
- `APP_PATHS`: A `|` delimited list of valid paths that should have the response status code set to `RESPONSE_CODE`.  All non-matching paths will be a 404.  Wildcards can be used.
- `RESPONSE_CODE`: The default HTTP response code.
- `USE_HTTPS`: Determines if the server should listen with `https` or `http`.  The value of `https` will be https using the dummy certs in the container, all other values will be http.
- `RESPONSE_HEADERS`: A string representation of JSON that is all headers that should be added to the response.  This can be used to provide custom response headers to match upstream server functionality.

**NOTE:** The expectation for the SnowGlobe project is that this container should not be manually called.  The idea is to use the [AppServiceCluster Class](https://github.com/Kroger-Technology/Snow-Globe/blob/master/src/main/java/com/kroger/oss/snowGlobe/AppServiceCluster.java)
to build your upstream cluster and the framework will build your compose map and container for you.