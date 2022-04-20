#!/usr/bin/env bash

# 环境准备
DirName=$(dirname "$0")
AppHome=$(realpath "$DirName")
RunHome="$AppHome"/var
LogFile="$RunHome"/app.log
StdFile="$RunHome"/std.log
PidFile="$RunHome"/run.pid
Param1="${1:-help}"
Param2="$2"

mkdir -p "$RunHome"
cd "$AppHome" || exit

if [[ -f "$PidFile" && -n $(cat "$PidFile") ]]; then
    PidText=$(cat "$PidFile")
    if [[ $(ps -p "$PidText" | wc -l) -eq 2 ]]; then
        Running="true"
    fi
fi

# 函数定义
boot_run() {
    echo mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
    nohup bash ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=dev >"$StdFile" 2>&1 &
    echo $! >"$PidFile"
    echo "The server is starting : $!"
}

build_if() {
    JarNums=$(find target -name '*.jar' -print | wc -l)
    if [[ "$1" == "-f" || "$JarNums" -eq 0 ]]; then
        echo mvn clean package
        bash ./mvnw clean package >"$StdFile" 2>&1
    fi
}

java_jar() {
    JarFile=$(find target -name '*.jar' -print | tail -1)
    if [[ -f "$JarFile" ]]; then
        echo java -Xms64m -Xmx256m -jar "$JarFile" --spring.profiles.active=prod
        nohup java -Xms64m -Xmx256m -jar "$JarFile" --spring.profiles.active=prod >"$StdFile" 2>&1 &
        echo $! >"$PidFile"
        echo "The server is starting : $!"
    elif [[ -f "$StdFile" ]]; then
        echo "Can't find jar file, std output : "
        cat "$StdFile"
    else
        echo "Can't find jar file and no error log"
    fi
}

kill_app() {
    if [[ "$1" == "-f" ]]; then
        echo kill -9 "$PidText"
        kill -9 "$PidText"
    else
        echo kill "$PidText"
        kill "$PidText"
    fi
    echo "The server is stopping"
    rm "$PidFile"
}

# 参数解析
case "$Param1" in
d | dd | dev)
    if [[ "$Running" == "true" ]]; then
        kill_app
        sleep 1
    fi
    boot_run
    sleep 1
    tail -f "$StdFile"
    ;;
st | start)
    if [[ "$Running" == "true" ]]; then
        echo "The server cannot be started, it has already started : $PidText"
    else
        build_if "$Param2"
        java_jar
    fi
    ;;
qt | stop)
    if [[ "$Running" == "true" ]]; then
        kill_app "$Param2"
    else
        echo "The server is not running"
    fi
    ;;
rt | restart)
    if [[ "$Running" == "true" ]]; then
        kill_app
        sleep 1
    fi
    build_if -f
    java_jar
    ;;
vt | status)
    if [[ "$Running" == "true" ]]; then
        echo "The server is running : $PidText"
    else
        echo "The server is not running"
    fi
    ;;
log)
    if [[ "$Param2" == "-a" ]]; then
        less "$LogFile"
    else
        tail -f "$LogFile"
    fi
    ;;
std)
    if [[ "$Param2" == "-a" ]]; then
        less "$StdFile"
    else
        tail -f "$StdFile"
    fi
    ;;
*)
    echo "usage: bash app.sh [d|dd|dev]"
    echo "usage: bash app.sh [st|start] [-f]"
    echo "usage: bash app.sh [qt|stop] [-f]"
    echo "usage: bash app.sh [rt|restart]"
    echo "usage: bash app.sh [vt|status]"
    echo "usage: bash app.sh log [-a]"
    echo "usage: bash app.sh std [-a]"
    ;;
esac
