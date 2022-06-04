#!/bin/bash -ev

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=11.5.0.0a
    ;;
  latest)
    DB_VERSION=11.5.7.0a
    ;;
esac

docker run --pull=always --rm --name db2 --entrypoint /bin/sh ibmcom/db2:$DB_VERSION -c "cat /opt/ibm/db2/V*/java/db2jcc4.jar" > db2jcc4.jar

docker run --rm --name db2 --cap-add IPC_LOCK --cap-add IPC_OWNER -e 'instance_name=root' -e 'DB2INST1_PASSWORD=nflow' -e 'LICENSE=accept' -e 'DBNAME=nflow' --publish 50000:50000 --detach ibmcom/db2:$DB_VERSION db2start

fgrep -m1 'Setup has completed' <(timeout 240 docker logs -f db2)

#docker exec -it db2 su - db2inst1 -c '/opt/ibm/db2/V*/bin/db2 -tvs "CREATE DATABASE nflow USING CODESET UTF-8 TERRITORY us;"'
#docker exec -it db2 su - db2inst1 -c '/opt/ibm/db2/V*/bin/db2 -tvs "ACTIVATE DATABASE nflow;"'
