#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=11.5.0.0a
    ;;
  latest)
    DB_VERSION=11.5.7.0a
    ;;
esac

$tool run --pull=always --rm --name db2 --cap-add IPC_LOCK --cap-add IPC_OWNER -e PERSISTENT_HOME=false -e DB2INST1_PASSWORD=nflow -e LICENSE=accept -e DBNAME=nflow -e ARCHIVE_LOGS=false --publish 50000:50000 --detach ibmcom/db2:$DB_VERSION

fgrep -m1 'Setup has completed' <(timeout 240 $tool logs -f db2)
