#!/bin/bash -ev

VER=11.5.0.0a

docker run --rm --name db2 --entrypoint /bin/sh ibmcom/db2:$VER -c "cat /opt/ibm/db2/V*/java/db2jcc4.jar" > db2jcc4.jar

docker run --rm --name db2 --cap-add IPC_LOCK --cap-add IPC_OWNER -e 'instance_name=root' -e 'DB2INST1_PASSWORD=nflow' -e 'LICENSE=accept' -e 'DBNAME=nflow' --publish 50000:50000 --detach ibmcom/db2:$VER db2start

echo "waiting for DB2 to start"
fgrep -m1 'Setup has completed' <(timeout 120 docker logs -f db2)
echo "DB2 started"

#docker exec -it db2 su - db2inst1 -c '/opt/ibm/db2/V*/bin/db2 -tvs "CREATE DATABASE nflow USING CODESET UTF-8 TERRITORY us;"'
#docker exec -it db2 su - db2inst1 -c '/opt/ibm/db2/V*/bin/db2 -tvs "ACTIVATE DATABASE nflow;"'
