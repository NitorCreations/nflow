#!/bin/bash -ev

VER=10.7
if [[ "$1" == 11 ]]; then
  VER=10.2 # supported until may/2022
fi

docker run --pull=always --rm --name mariadb -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mariadb:$VER --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

fgrep -m1 'ready for connections' <(timeout 120 docker logs -f mariadb 2>&1)
