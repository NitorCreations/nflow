#!/bin/bash -ev

docker run --rm --name db2 --entrypoint /bin/sh ibmcom/db2express-c:latest -c "cat /home/db2inst1/sqllib/java/db2jcc4.jar" > db2jcc4.jar

docker run --rm --name db2 -e 'DB2INST1_PASSWORD=nflow' -e 'LICENSE=accept' --publish 50000:50000   --detach ibmcom/db2express-c:latest db2start
sleep 5

docker exec -it db2 su - db2inst1 -c '/home/db2inst1/sqllib/bin/db2 -tvs "CREATE DATABASE nflow USING CODESET UTF-8 TERRITORY us;"'
docker exec -it db2 su - db2inst1 -c '/home/db2inst1/sqllib/bin/db2 -tvs "ACTIVATE DATABASE nflow;"'
