#!/usr/bin/env bash

ID=$1

if [ `docker ps | grep -c $ID` -ne 0 ]
then
    docker rm --force=true $ID 1>/dev/null 2>/dev/null
fi