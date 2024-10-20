#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    IMAGE=docker.io/gvenzl/oracle-xe
    DB_VERSION=18-slim
    ;;
  latest)
    IMAGE=docker.io/gvenzl/oracle-free
    DB_VERSION=23.5-slim
    ;;
esac

$tool run --pull=always --rm --name oracle --publish 1521:1521 -e ORACLE_DATABASE=nflow -e ORACLE_PASSWORD=nflow -e APP_USER=nflow -e APP_USER_PASSWORD=nflow --detach $IMAGE:$DB_VERSION
grep -F -m1 'DATABASE IS READY' <(timeout 240 $tool logs -f oracle)
