#!/usr/bin/env bash
ID=$1
CLUSTER_NAME=$2
INSTANCE_NUMBER=$3
RESPONSE_CODE=$4
APP_PATHS=$5
PORT=$6
RESPONSE_HEADERS=$7
USE_HTTPS=$8

if [ `docker ps | grep -c $ID` -ne 0 ]
then
    echo -n "Shutting down existing container $ID..."
    docker rm --force=true $ID 1>>/dev/null 2>>/dev/null
    echo "done."
fi
echo -n "Starting container: $ID..."
docker run --detach=true \
    --env INSTANCE_NUMBER=$INSTANCE_NUMBER \
    --env CLUSTER_NAME=$CLUSTER_NAME \
    --env APP_PATHS=$APP_PATHS \
    --env RESPONSE_CODE=$RESPONSE_CODE \
    --env RESPONSE_HEADERS="$RESPONSE_HEADERS" \
    --env USE_HTTPS=${USE_HTTPS} \
    --volume `pwd`:/home/default/myapp \
    --name $ID \
    --expose 3000 \
    -p $PORT:3000 \
    docker.kroger.com/library/tinynode --minUptime 1000 --spinSleepTime 1000 myapp/service.js

# wait for container to start accepting traffic.
RETURN=1
ATTEMPTS=0
while [ ${RETURN} -ne 0 ]
do
    if [ ${ATTEMPTS} -gt 20 ] ; then
        docker logs ${NAME}
        exit 1
    fi
    sleep 0.25
    curl -I http://localhost:${PORT}/INTERNALHEALTHCHECKFORSTARTUP 1>>/dev/null 2>>/dev/null
    RETURN=$(echo $?)
    ATTEMPTS=$(($ATTEMPTS + 1))
done
