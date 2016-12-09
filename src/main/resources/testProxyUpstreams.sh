#!/usr/bin/env bash

ENV=$1

echo -n "Starting container: $NAME..."

OVERRIDES_PARAM=""

if [ -z "${ENV}" ]; then
    echo "An environment must be specified for this test."
    exit 1;
fi

if [[ "localDev" -eq "${ENV}" ]]; then
    STATIC_ROUTE_OPT="--volume `pwd`/../../nginx/env/localDev_static_routes.conf:/etc/nginx/Link2StaticRoutes.conf:ro"
else
    STATIC_ROUTE_OPT="--volume `pwd`/../../nginx/Link2StaticRoutes.conf:/etc/nginx/Link2StaticRoutes.conf:ro"
fi

docker run \
  --rm \
  --add-host localApp:127.0.0.1 \
  --volume `pwd`/../../nginx/nginx.conf:/etc/nginx/nginx.conf:ro \
  --volume `pwd`/../../nginx/servers.conf:/etc/nginx/servers.conf:ro \
  --volume `pwd`/../../nginx/changeWcsCookiePathsToBrowser.lua:/etc/nginx/changeWcsCookiePathsToBrowser.lua:ro \
  --volume `pwd`/../../nginx/bannedhosts.conf:/etc/nginx/bannedhosts.conf:ro \
  --volume `pwd`/../../nginx/temp-bannedhosts.conf:/etc/nginx/temp-bannedhosts.conf:ro \
  --volume `pwd`/../../nginx/proxies:/etc/nginx/proxies:ro \
  ${STATIC_ROUTE_OPT} \
  --volume `pwd`/../../nginx/mime.types:/etc/nginx/mime.types:ro \
  --volume $(pwd)/../../nginx/env/${ENV}_environment.conf:/etc/nginx/env/Link2Environment.conf:ro \
  --volume `pwd`/../../test/resources/internal.cert:/etc/nginx/certs/internal.cert:ro \
  --volume `pwd`/../../test/resources/internal.key:/etc/nginx/certs/internal.key:ro \
  --volume `pwd`/../../static:/usr/nginx/webapps/nginx/conf/html:ro \
  --volume $(pwd)/../../nginx/env-overrides/${ENV}_route_overrides.conf:/etc/nginx/env-overrides/Link2EnvOverrides.conf:ro \
  docker.kroger.com/library/nginx:1.11.3 /usr/sbin/nginx -t

