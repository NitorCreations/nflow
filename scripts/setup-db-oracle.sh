#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
#    DB_VERSION=18.4.0-xe
    DB_VERSION=18-slim
    ;;
  latest)
#    DB_VERSION=21.3.0-xe
    DB_VERSION=21-slim
    ;;
esac

#$tool run --pull=always --rm --name oracle --publish 1521:1521 -e ORACLE_PWD=nflow --detach container-registry.oracle.com/database/express:$DB_VERSION
#$tool cp oracle:/opt/oracle/product/21c/dbhomeXE/jdbc/lib/ojdbc11.jar ojdbc11.jar

#fgrep -m1 'DATABASE IS READY' <(timeout 240 $tool logs -f oracle)

# create user nflow identified by nflow;
# grant CONNECT, CREATE SESSION, ALTER SESSION, CREATE MATERIALIZED VIEW, CREATE PROCEDURE, CREATE PUBLIC SYNONYM, CREATE ROLE, CREATE SEQUENCE, CREATE SYNONYM, CREATE TABLE, CREATE TRIGGER, CREATE TYPE, CREATE VIEW, UNLIMITED TABLESPACE to nflow;


$tool run --pull=always --rm --name oracle --publish 1521:1521 -e ORACLE_PASSWORD=nflow -e APP_USER=nflow -e APP_USER_PASSWORD=nflow --detach gvenzl/oracle-xe:$DB_VERSION
fgrep -m1 'DATABASE IS READY' <(timeout 240 $tool logs -f oracle)
