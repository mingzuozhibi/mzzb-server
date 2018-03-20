#!/usr/bin/env bash

cd $(cd `dirname $0`; pwd)

echo 'Try Stop Mzzb Server'
sh ./stop-server.sh

echo 'Build Mzzb Server'
mvn clean package > /dev/null

echo 'Start Mzzb Server'
nohup java -jar target/*.jar --target.name=mzzb-server --spring.profiles.active=pro > /dev/null 2>&1 &
