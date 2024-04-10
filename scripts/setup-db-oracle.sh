#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
# could not yet get working
#IMAGE=container-registry.oracle.com/database/free
IMAGE=gvenzl/oracle-xe
case $DB_VERSION in
  old)
    # could not yet get working
    #DB_VERSION=18.4.0-xe
    #IMAGE=container-registry.oracle.com/database/express
    DB_VERSION=18-slim
    ;;
  latest)
    # could not yet get working
    #IMAGE=container-registry.oracle.com/database/free
    #DB_VERSION=23.3.0.0
    DB_VERSION=21-slim
    ;;
esac

#$tool run --pull=always --rm --name oracle --publish 1521:1521 -e ORACLE_PWD=nflow --detach container-registry.oracle.com/database/express:$DB_VERSION
#$tool cp oracle:/opt/oracle/product/21c/dbhomeXE/jdbc/lib/ojdbc11.jar ojdbc11.jar

#grep -F -m1 'DATABASE IS READY' <(timeout 240 $tool logs -f oracle)

# create user nflow identified by nflow;
# grant CONNECT, CREATE SESSION, ALTER SESSION, CREATE MATERIALIZED VIEW, CREATE PROCEDURE, CREATE PUBLIC SYNONYM, CREATE ROLE, CREATE SEQUENCE, CREATE SYNONYM, CREATE TABLE, CREATE TRIGGER, CREATE TYPE, CREATE VIEW, UNLIMITED TABLESPACE to nflow;


$tool run --pull=always --rm --name oracle --publish 1521:1521 -e ORACLE_PASSWORD=nflow -e APP_USER=nflow -e APP_USER_PASSWORD=nflow --detach $IMAGE:$DB_VERSION
grep -F -m1 'DATABASE IS READY' <(timeout 240 $tool logs -f oracle)
