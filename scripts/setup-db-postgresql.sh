#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=10 # supported until nov/2022
    ;;
  latest)
    DB_VERSION=14
    ;;
esac

$tool run --pull=always  --rm --name postgres -e POSTGRES_DB=nflow -e POSTGRES_USER=nflow -e POSTGRES_PASSWORD=nflow --publish 5432:5432 --detach postgres:$DB_VERSION -c fsync=off -c full_page_writes=off

fgrep -m1 'listening on IPv4' <(timeout 240 $tool logs -f postgres 2>&1)
