#!/usr/bin/env bash

pid=$(ps -x | grep 'target.name=mzzb-server' | grep -v grep | awk '{print $1}')

if [ -n "${pid}" ]; then
    echo 'Mzzb Server Pid:' ${pid}
fi

if [ -z "${pid}" ]; then
    echo 'Mzzb Server Not Running'
fi
