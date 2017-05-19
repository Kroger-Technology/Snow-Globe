#!/bin/bash
docker run --privileged -it -v `pwd`:/snowGlobe docker:dind /bin/sh
apk update
apk add git openssh-client ca-certificates openjdk8 bash py-pip
pip install docker-compose
echo "#############################"
echo ""
echo " Welcome!  To run the tests.tests, run `./gradlew test`"