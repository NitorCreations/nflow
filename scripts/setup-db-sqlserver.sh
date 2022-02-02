#!/bin/bash -ev

VER=2019-latest
if [[ "$1" == 11 ]]; then
  VER=2017-latest # supported until 2027
fi

docker run --pull=always  --rm --name mssql -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=passWord1%' --publish 1433:1433 --detach mcr.microsoft.com/mssql/server:$VER

sleep 5

sqlcmd="docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x"
$sqlcmd -Q "create database nflow"
$sqlcmd -d nflow -Q "create login [nflow] with password='nFlow42%', default_database=[nflow]"
$sqlcmd -d nflow -Q "create user [nflow] for login [nflow] with default_schema=[nflow]"
$sqlcmd -d nflow -Q "create schema nflow authorization nflow"
$sqlcmd -d nflow -Q "grant connect to [nflow]"
$sqlcmd -d nflow -Q "alter role db_datareader add member nflow"
$sqlcmd -d nflow -Q "alter role db_datawriter add member nflow"
$sqlcmd -d nflow -Q "grant all to [nflow]"
