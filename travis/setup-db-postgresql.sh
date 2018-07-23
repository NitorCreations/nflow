#!/bin/bash -ev

psql -c "create user nflow with password 'nflow';" -U postgres
psql -c "create database nflow owner nflow;" -U postgres
