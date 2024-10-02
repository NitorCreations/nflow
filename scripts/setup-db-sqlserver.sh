#!/bin/bash -ev

tool=$(command -v podman)
[ -z "$tool" ] && tool=$(command -v docker)
[ -z "$tool" ] && echo "podman or docker required" && exit 1

DB_VERSION=${DB_VERSION:-latest}
case $DB_VERSION in
  old)
    DB_VERSION=2017-latest # supported until 2027
    SQLCMD_EXEC=/opt/mssql-tools/bin/sqlcmd
    ;;
  latest)
    DB_VERSION=2022-latest
    SQLCMD_EXEC="/opt/mssql-tools18/bin/sqlcmd -C"
    ;;
esac

$tool run --pull=always  --rm --name mssql -e 'ACCEPT_EULA=Y' -e 'MSSQL_SA_PASSWORD=passWord1%' --publish 1433:1433 --detach mcr.microsoft.com/mssql/server:$DB_VERSION

grep -F -m1 'Recovery is complete' <(timeout 240 $tool logs -f mssql 2>&1)

sqlcmd="$tool exec -t mssql $SQLCMD_EXEC -S localhost -U sa -P passWord1% -e -x"
$sqlcmd -Q "create database nflow"
$sqlcmd -d nflow -Q "create login [nflow] with password='FC8%1knw', default_database=[nflow]"
$sqlcmd -d nflow -Q "create user [nflow] for login [nflow] with default_schema=[nflow]"
$sqlcmd -d nflow -Q "create schema nflow authorization nflow"
$sqlcmd -d nflow -Q "grant connect to [nflow]"
$sqlcmd -d nflow -Q "alter role db_datareader add member nflow"
$sqlcmd -d nflow -Q "alter role db_datawriter add member nflow"
$sqlcmd -d nflow -Q "grant all to [nflow]"
