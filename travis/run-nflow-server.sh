#!/usr/bin/bash

echo Install nflow server
git clone https://github.com/NitorCreations/nflow.git
cd nflow
mvn package -DskipTests

echo Run nflow server
java -jar nflow-tests/target/nflow-tests-*-SNAPSHOT.jar &

echo Wait a bit for nflow server to start
sleep 10
