#!/usr/bin/env bash

echo 'Try Stop Mzzb Admin'
sh ./stop-server.sh

cd $(cd `dirname $0`; pwd)

echo 'Build Mzzb Admin'
mvn clean package > /dev/null

echo 'Start Mzzb Admin'
nohup java -jar target/*.jar --target.name=mzzb-admin > /dev/null 2>&1 &
