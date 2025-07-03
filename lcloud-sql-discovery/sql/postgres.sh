#!/bin/sh

#
# Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
#
export MSYS_NO_PATHCONV=1
docker stop demo_db
docker rm demo_db
#docker run -i --rm postgres cat /usr/share/postgresql/postgresql.conf.sample > my-postgres.conf
docker run --name demo_db -p 5432:5432  -v "$PWD/my-postgres.conf":/etc/postgresql/postgresql.conf -e POSTGRES_USER=user -e POSTGRES_PASSWORD=user -e POSTGRES_DB=demo -d postgres:latest -c 'config_file=/etc/postgresql/postgresql.conf'
