#!/bin/bash -ev

VER=8.0
if [[ "$1" == 11 ]]; then
  VER=5.7 # supported until oct/2023
fi

docker run --pull=always  --rm --name mysql -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mysql:$VER --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

fgrep -m1 'ready for connections' <(timeout 120 docker logs -f mysql 2>&1)
