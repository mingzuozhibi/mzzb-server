#!/usr/bin/env bash

mvn clean package
java -jar target/*.jar > /dev/null 2>&1 &
