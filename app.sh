#!/usr/bin/env bash

##
# 定位应用所在目录
##

APP_HOME=$(cd `dirname $0`; pwd)
APP_NAME=${APP_HOME##*/}

cd ${APP_HOME}

##
# 定义应用启动命令
##

JAVA_OPTS="-Xms64m -Xmx128m -XX:MaxMetaspaceSize=1024m"
JAR_OPTS="--app.name=${APP_NAME} --spring.profiles.active=pro"
LOG_FILE="target/webapp.log"

if [ -f ./RUN_OPTS ]; then
  source ./RUN_OPTS
fi

start_app() {
  if [ -z "$(jar_file)" ]; then
    build_app
  fi
  if [ -z "$(jar_file)" ]; then
    echo "启动应用失败：未找到JAR文件"
  else
    echo "正在启动应用"
    echo "java ${JAVA_OPTS} -jar $(jar_file) ${JAR_OPTS} >${LOG_FILE} 2>&1"
    nohup java ${JAVA_OPTS} -jar $(jar_file) ${JAR_OPTS} >${LOG_FILE} 2>&1 & exit 0
  fi
}

kill_app() {
  echo "正在停止应用: kill $(app_pid)"
  kill $(app_pid)
}

kill_app_focus() {
  echo "正在停止应用: kill -9 $(app_pid)"
  kill -9 $(app_pid)
}

build_app() {
  echo "正在构建应用"
  mvn clean package >/dev/null
}

app_pid() {
  echo $(ps -x | grep "app.name=${APP_NAME}" | grep -v grep | awk '{print $1}')
}

jar_file() {
  echo $(ls target/*.jar | xargs -n 1 | tail -1) 2>/dev/null
}

unsupport() {
  echo "不支持 '$@' 命令，请使用 help 命令查看帮助"
}

##
# 执行指定脚本命令
##

if [ $# = 1 ]; then
  case $1 in
    help)
      echo "usage: run.sh [<command>] [<args>]"
      echo ""
      echo "support commands:"
      echo ""
      echo "   st                启动应用"
      echo "   st -f             启动应用（重新构建，并重新启动）"
      echo "   qt                停止应用"
      echo "   qt -f             停止应用（强制停止）"
      echo "   vt                运行状态"
      echo ""
      echo "   sl                tail -f <Log>"
      echo "   sl <pattern>      grep <pattern> Log"
      echo "   sl -a             less <Log>"
      echo "   sl -v             vim Log"
      echo ""
      echo "   fe -m             git fetch && git reset --hard origin/master"
      echo "   fe -d             git fetch && git reset --hard origin/develop"
      echo "   fe <branch>       git fetch && git reset --hard origin/<branch>"
      echo ""
      echo "   jm -h             jmap -heap <pid>"
      echo "   help              显示本帮助"
      echo ""
      exit 0
    ;;
    st)
      if [ -n "$(app_pid)" ]; then
        echo "应用已经启动"
        exit 1
      else
        start_app
        exit 0
      fi
    ;;
    qt)
      if [ -z "$(app_pid)" ]; then
        echo "应用并未运行"
        exit 1
      else
        kill_app
        exit 0
      fi
    ;;
    vt)
      if [ -n "$(app_pid)" ]; then
        echo "应用运行中, PID=$(app_pid)"
        exit 0
      else
        echo "应用未运行"
        exit 0
      fi
    ;;
    sl)
      echo "正在打开日志: tail -f ${LOG_FILE}"
      tail -f ${LOG_FILE}
      exit 0
    ;;
  esac
elif [ $# = 2 ]; then
  case $1 in
    st)
      if [ $2 = "-f" ]; then
        build_app

        if [ -n "$(app_pid)" ]; then
          kill_app_focus
        fi

        start_app
        exit 0
      fi
    ;;
    qt)
      if [ $2 = "-f" ]; then
        if [ -z "$(app_pid)" ]; then
          echo "应用并未运行"
          exit 1
        else
          kill_app_focus
          exit 0
        fi
      fi
    ;;
    sl)
      case $2 in
        -a) less ${LOG_FILE}; exit 0;;
        -v) vim ${LOG_FILE}; exit 0;;
        *) grep $2 ${LOG_FILE}; exit 0;;
      esac
    ;;
    fe)
      case $2 in
        -m) git fetch && git reset --hard origin/master; exit 0;;
        -d) git fetch && git reset --hard origin/develop; exit 0;;
        *) git fetch && git reset --hard origin/$2; exit 0;;
      esac
    ;;
    jm)
      pid=$(app_pid)
      if [ -z "${pid}" ]; then echo "应用并未运行"; exit 1; fi
      case $2 in
        -h) jmap -heap ${pid}; exit 0;;
      esac
    ;;
  esac
fi

unsupport $@
exit 1
