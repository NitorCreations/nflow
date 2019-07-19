#!/bin/bash -ev

mysql -v -e "create database nflow character set utf8mb4;" -u root
mysql -v -e "create user 'nflow'@'%' identified by 'nflow';" -u root
mysql -v -e "create user 'nflow'@'localhost' identified by 'nflow';" -u root
mysql -v -e "grant all on nflow.* TO 'nflow'@'%';" -u root
mysql -v -e "grant all on nflow.* TO 'nflow'@'localhost';" -u root
mysql -v -e "flush privileges;" -u root
