#!/usr/bin/env bash

# 环境准备
DirName=$(dirname "$0")
AppHome=$(realpath "$DirName")
RunHome=$AppHome/var
LogFile="$RunHome/app.log"
StdFile="$RunHome/std.log"
PidFile="$RunHome/run.pid"
ParamCmd=${1:-help}

mkdir -p "$RunHome"
cd "$AppHome" || exit

if [[ -f $PidFile && -n $(cat "$PidFile") ]]; then
    PidText=$(cat "$PidFile")
    if [[ $(ps -p "$PidText" | wc -l) -eq 2 ]]; then
        Running="true"
    fi
fi

# 函数定义
boot_run() {
    if [[ $2 == "-f" ]]; then
        echo "bash ./mvnw clean spring-boot:run -Dspring-boot.run.profiles=$1 >$StdFile 2>&1 &"
        nohup bash ./mvnw clean spring-boot:run -Dspring-boot.run.profiles="$1" >"$StdFile" 2>&1 &
    else
        echo "bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=$1 >$StdFile 2>&1 &"
        nohup bash ./mvnw spring-boot:run -Dspring-boot.run.profiles="$1" >"$StdFile" 2>&1 &
    fi
    echo $! >"$PidFile"
    echo "The server is starting : $!"
}

kill_app() {
    echo "kill $PidText"
    kill "$PidText"
    echo "The server is stopping"
    rm "$PidFile"
}

# 参数解析
case $ParamCmd in
d | dev)
    if [[ $Running == "true" ]]; then
        kill_app
        sleep 1
    fi
    boot_run dev -f
    sleep 1
    tail -f "$StdFile"
    ;;
st | start)
    if [[ $Running == "true" ]]; then
        echo "The server cannot be started, it has already started : $PidText"
    else
        boot_run pro
    fi
    ;;
qt | stop)
    if [[ $Running == "true" ]]; then
        kill_app
    else
        echo "The server is not running"
    fi
    ;;
rt | restart)
    if [[ $Running == "true" ]]; then
        kill_app
        sleep 1
    fi
    boot_run pro -f
    ;;
vt | status)
    if [[ $Running == "true" ]]; then
        echo "The server is running : $PidText"
    else
        echo "The server is not running"
    fi
    ;;
sl | show)
    if [[ $2 == "std" ]]; then
        less "$StdFile"
    else
        less "$LogFile"
    fi
    ;;
sf | tail)
    if [[ $2 == "std" ]]; then
        tail -f "$StdFile"
    else
        tail -f "$LogFile"
    fi
    ;;
*)
    echo "usage: bash app.sh [d|dev]"
    echo "usage: bash app.sh [st|start]"
    echo "usage: bash app.sh [qt|stop]"
    echo "usage: bash app.sh [rt|restart]"
    echo "usage: bash app.sh [vt|status]"
    echo "usage: bash app.sh [sl|show] [std|app]"
    echo "usage: bash app.sh [sf|tail] [std|app]"
    ;;
esac
