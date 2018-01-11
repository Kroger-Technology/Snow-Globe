#!/bin/bash

echo
echo --------------------------------------------------------
echo
echo Checking required dependencies:
echo
FOUND_JAVA=$(type -p java)
if [[ "$FOUND_JAVA" ]]; then
    JAVA_PATH=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    JAVA_PATH="$JAVA_HOME/bin/java"
fi

if [[ -n "$JAVA_PATH" ]]; then
    JAVA_VERSION=$("$JAVA_PATH" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -n "Java:   "
    if [[ "$JAVA_VERSION" > "1.7" ]]; then
        echo "'${JAVA_VERSION}' (CHECK)"
    else
        echo "'${JAVA_VERSION}' (FAIL: This needs to be 1.8 or greater.)"
        exit 1
    fi
fi

DOCKER_VERSION=$(docker --version 2>&1 | awk -F "[, ]" '{print $3}')
echo -n "Docker: "
if [[ "$DOCKER_VERSION" > "1.10" ]]; then
    echo "'${DOCKER_VERSION}' (CHECK)"
else
    echo "'${DOCKER_VERSION}' (FAIL: This needs to be 1.11.0 or greater.)"
    exit 1
fi

DOCKER_COMPOSE_VERSION=$(docker-compose --version 2>&1 | awk -F "[, ]" '{print $3}')
echo -n "Docker Compose: "
if [[ "$DOCKER_COMPOSE_VERSION" > "1.10" ]]; then
    echo "'${DOCKER_COMPOSE_VERSION}' (CHECK)"
else
    echo "'${DOCKER_COMPOSE_VERSION}' (FAIL: This needs to be 1.10.0 or greater.)"
    exit 1
fi

echo
echo "Looks like you are set to go!"
echo


