#!/usr/bin/env bash

pid=$(ps -x | grep 'target.name=mzzb-server' | grep -v grep | awk '{print $1}')

if [ -n "${pid}" ]; then
    kill -9 ${pid}
    echo 'Stop Mzzb Server'
fi

if [ -z "${pid}" ]; then
    echo 'Mzzb Server Not Running'
fi
