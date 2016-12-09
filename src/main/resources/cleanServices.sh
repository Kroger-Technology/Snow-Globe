#!/bin/bash
docker rm -f $(docker ps -aq -f name=CLUSTER-*) 1>/dev/null 2>/dev/null