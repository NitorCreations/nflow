#!/bin/bash -ev

if [[ -n "$TRAVIS" ]]; then
  psql -c "create user nflow with password 'nflow';" -U postgres
  psql -c "create database nflow owner nflow;" -U postgres
  exit 0
fi

VER=14
if [[ "$1" == 8 ]]; then
  VER=10 # supported until nov/2022
fi

docker run --rm --name postgres -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e POSTGRES_DB=nflow -e POSTGRES_USER=nflow -e POSTGRES_PASSWORD=nflow --publish 5432:5432 --detach postgres:$VER -c fsync=off -c full_page_writes=off

fgrep -m1 'listening on IPv4' <(docker logs -f postgres 2>&1)
