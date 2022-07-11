#!/bin/bash -ev

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=5.7 # supported until oct/2023
    ;;
  latest)
    DB_VERSION=8.0
    ;;
esac

docker run --pull=always  --rm --name mysql -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mysql:$DB_VERSION --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

fgrep -m1 'ready for connections' <(timeout 240 docker logs -f mysql 2>&1)
