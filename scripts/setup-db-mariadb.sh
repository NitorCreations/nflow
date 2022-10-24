#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=10.3 # supported until may/2023
    ;;
  latest)
    DB_VERSION=10.9
    ;;
esac

$tool run --pull=always --rm --name mariadb -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mariadb:$DB_VERSION --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --sync-binlog=0 --innodb-flush-log-at-trx-commit=2

fgrep -m1 'ready for connections' <(timeout 240 $tool logs -f mariadb 2>&1)
