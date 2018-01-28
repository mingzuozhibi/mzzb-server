#!/usr/bin/env bash

mvn clean package
java -jar target/*.jar --spring.profiles.active=pro
