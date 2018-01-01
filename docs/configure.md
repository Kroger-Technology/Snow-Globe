# Configuring Snow-Globe

Snow-Globe is configured through a YML file.  The default file that is used is `snow-globe.yml`.  Below are the different options available.

SnowGlobe will crawl your configuration for each test and build the temporary upstreams.  So to be able to do that, it
 needs to have the upstreams define in a separate file that are _not_ included in the configuration.  We recommend that
 you have your upstreams in a separate directory.  That makes it more simple to include/exclude files for the tests.

You can see an example Nginx setup in the [exampleUsage](https://github.com/Kroger-Technology/Snow-Globe/blob/master/exampleUsage/snow-globe.yml) directory.


The first part of the configuration is how we map your configuration files into the docker container.  We use `nginx.volume.mounts` to map your local
files into the path on the docker image.  Here is an example:

```yaml
nginx.volume.mounts:
  - "src/nginx/nginx.conf:/etc/nginx/nginx.conf"
```
The `nginx.volume.mounts` is an array of mappings.  You can mount a single file, a directory, or a wildcard.  One
thing to note is that you can't map your upstream file.  SnowGlobe is going to build one for you and put it in.  Here
is an example mounts with different mapping techniques.

```yaml
nginx.volume.mounts:
    - "src/nginx/*:/webApps/nginx/conf/"
    - "src/nginx/html:/webApps/nginx/conf/html"
    - "src/nginx/env/prod/*:/webApps/nginx/conf/env/prod/"
    - "src/nginx/env/prod/certs/:/webApps/nginx/conf/env/prod/certs/"
    - "src/nginx/env/prod/locations/:/webApps/nginx/conf/env/prod/locations/"
    - "src/nginx/env/prod/servers/:/webApps/nginx/conf/env/prod/servers/"
    - "src/scripts/startTest.sh:/startTest.sh"
```

Next up, you need to help SnowGlobe tell us where you put you upstream file(s).  The field is `upstream.file.path`.  This is done because SnowGlobe will 
generate one for you and drop it in for you.  This is the file location on the deployed configuration, not the path
for the source code. This value is optional and only needed if the configuration contains upstream blocks in the configuration.

```yaml
upstream.file.path: "/etc/nginx/upstreams.conf"
```

The next two fields `nginx.source.base.directory` and `nginx.deploy.base.directory` define the "base" directory for you configuration.  It is popular to
use relative paths for includes and such.  This gives SnowGlobe advice on how to interpret includes using a relative path:

```yaml
nginx.source.base.directory: "src/integrationTestNginxConfig/"
nginx.deploy.base.directory: "/etc/nginx/"
```

After that, we need to know what container you are using to run Nginx. This field is `nginx.container`.  This is an optional parameter with a default value being the one below:

```yaml
nginx.container: "nginx"
```

This next part is the port mapping for traffic.  This field is: `nginx.url.port.mapping`  Most people send HTTPS -> 443 and HTTP -> 80.   But we have had
cases where this is not true.  This section allows you to define the traffic and how you want to map it.  You can
define multiple patterns for the url and which port you route traffic.  This will choose the first matching url pattern
defined in the list.  Order is important here.

This value is optional an below is the default setting:

```yaml
nginx.url.port.mapping:
  - https:
      pattern: "https:.*"
      port: 443
  - http:
      pattern: "http:.*"
      port: 80
```

This next field is the docker container to use for the upstream bounce service.  This is the container that will pretend
to be your upstream server and will bounce back the request in the body of the response.  This allows the testing
framework to assert on all parts of the request and response.

This is optional and below is the default:

```yaml
upstream.bounce.image: "krogersnowglobe/upstream-bounce-service:latest"
```

This next field defines how to start nginx.   You may have a custom script that you
use and this is where you run that.

This is optional and below is the default value:

```yaml
nginx.start.command: ["nginx", "-g", "'daemon off;'"]
```

This next field tells SnowGlobe where to search when inspecting the configuration.  Many times, this is
just the nginx.conf or base file that is used for the configuration.  If you deployment is more than just copying
a directory, you will need to tell SnowGlobe where all of the moved files are.  

```yaml
nginx.env.config.files:
    - "/src/integrationTestNginxConfig/nginx.conf"
```


This field will when set to true will output all logs from the framework and docker containers.
This is optional and below is the default value:

```yaml
snowglobe.log.output:  false
```
This field will preserve the compose and upstream files for help in investigating a problem:
This is optional and below is the default value:

 ```yaml
snowglobe.preserve.temp.files: false
```

This last field will add upstream zones if you are using Nginx Plus.
 
This is optional and below is the default value:

```yaml
nginx.define.upstream.zones: false
```

### [Next: Write the Tests](https://kroger-technology.github.io/Snow-Globe/tests)