#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=11.5.0.0a
    MAJORVER=V11.5
    ;;
  latest)
    DB_VERSION=11.5.7.0a
    MAJORVER=V11.5
    ;;
esac

$tool run --pull=always --rm --name db2 --cap-add IPC_LOCK --cap-add IPC_OWNER -e 'instance_name=root' -e 'DB2INST1_PASSWORD=nflow' -e 'LICENSE=accept' -e 'DBNAME=nflow' --publish 50000:50000 --detach ibmcom/db2:$DB_VERSION
$tool cp db2:/opt/ibm/db2/$MAJORVER/java/db2jcc4.jar db2jcc4.jar

fgrep -m1 'Setup has completed' <(timeout 240 $tool logs -f db2)

#$tool exec -it db2 su - db2inst1 -c '/opt/ibm/db2/V*/bin/db2 -tvs "CREATE DATABASE nflow USING CODESET UTF-8 TERRITORY us;"'
#$tool exec -it db2 su - db2inst1 -c '/opt/ibm/db2/V*/bin/db2 -tvs "ACTIVATE DATABASE nflow;"'
