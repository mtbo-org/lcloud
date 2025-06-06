#!/bin/sh

#
# Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
#

docker stop demo_db
docker rm demo_db
docker run --name demo_db -p 5432:5432 -e POSTGRES_USER=user -e POSTGRES_PASSWORD=user -e POSTGRES_DB=demo -d postgres:latest
