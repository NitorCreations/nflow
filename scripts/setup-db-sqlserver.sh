#!/bin/bash -ev

docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=passWord1%' --publish 1433:1433  --name mssql --detach mcr.microsoft.com/mssql/server:latest

sleep 5

docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -Q "create database nflow"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "create login [nflow] with password='nFlow42%', default_database=[nflow]"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "create user [nflow] for login [nflow] with default_schema=[nflow]"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "create schema nflow authorization nflow"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "grant connect to [nflow]"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "alter role db_datareader add member nflow"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "alter role db_datawriter add member nflow"
docker exec -t mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P passWord1% -e -x -d nflow -Q "grant all to [nflow]"
