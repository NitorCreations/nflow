#!/bin/bash -ev

if [[ -n "$TRAVIS" ]]; then
  mysql -v -e "create database nflow character set utf8mb4;" -u root
  mysql -v -e "create user 'nflow'@'%' identified by 'nflow';" -u root
  mysql -v -e "create user 'nflow'@'localhost' identified by 'nflow';" -u root
  mysql -v -e "grant all on nflow.* TO 'nflow'@'%';" -u root
  mysql -v -e "grant all on nflow.* TO 'nflow'@'localhost';" -u root
  mysql -v -e "flush privileges;" -u root
  exit 0
fi


VER=8.0
if [[ "$1" == 8 ]]; then
  VER=5.7 # supported until oct/2023
fi

docker run --rm --name mysql -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=nflow -e MYSQL_USER=nflow -e MYSQL_PASSWORD=nflow --publish 3306:3306 --detach mysql:$VER --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

fgrep -m1 'ready for connections' <(timeout 120 docker logs -f mysql 2>&1)
