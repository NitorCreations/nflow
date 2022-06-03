#!/bin/bash -ev

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=10.2 # supported until may/2022
    ;;
  latest)
    DB_VERSION=10.8
    ;;
esac

docker run --pull=always --rm --name mariadb -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mariadb:$DB_VERSION --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

fgrep -m1 'ready for connections' <(timeout 120 docker logs -f mariadb 2>&1)
