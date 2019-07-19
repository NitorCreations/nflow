#!/bin/bash -ev

if [[ -n "$TRAVIS" ]]; then
  sudo service mysql stop || true
fi

docker run --rm --name mariadb -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mariadb:10.4 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

fgrep -m1 'ready for connections' <(timeout 120 docker logs -f mariadb 2>&1)
