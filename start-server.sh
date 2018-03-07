#!/usr/bin/env bash

sh ./stop-server.sh

mvn clean package
nohup java -jar target/*.jar --target.name=mzzb-server --spring.profiles.active=pro > /dev/null 2>&1 &
echo 'Start Mzzb Server'
