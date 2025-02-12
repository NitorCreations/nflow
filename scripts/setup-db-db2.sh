#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
# podman search --list-tags icr.io/db2_community/db2
case $DB_VERSION in
  old)
    DB_VERSION=11.5.5.1
    ;;
  latest)
    DB_VERSION=12.1.1.0
    ;;
esac

$tool run --pull=always --rm --name db2 --cap-add IPC_LOCK --cap-add IPC_OWNER -e PERSISTENT_HOME=false -e DB2INST1_PASSWORD=nflow -e LICENSE=accept -e DBNAME=nflow -e ARCHIVE_LOGS=false --publish 50000:50000 --detach icr.io/db2_community/db2:$DB_VERSION

grep -F -m1 'Setup has completed' <(timeout 240 $tool logs -f db2)
