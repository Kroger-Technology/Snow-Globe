#!/bin/bash
VERSION=1.0
docker build . -t docker.kroger.com/nginx/fake-upstream-service:${VERSION}