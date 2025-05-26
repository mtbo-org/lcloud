#!/bin/sh

#
# Copyright (c) 2025. Vladimir E. Koltunov, mtbo.org
#

docker stop h2_db
docker rm h2_db
MSYS_NO_PATHCONV=1  docker run --name h2_db -p 1521:1521 -p 81:81 -v "$PWD/h2-data:/opt/h2-data" -e H2_OPTIONS=-ifNotExists oscarfonts/h2
