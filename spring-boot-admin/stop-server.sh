#!/usr/bin/env bash

pid=$(ps -x | grep 'target.name=mzzb-admin' | grep -v grep | awk '{print $1}')

if [ -n "${pid}" ]; then
    kill -9 ${pid}
    echo 'Stop Mzzb Admin'
fi

if [ -z "${pid}" ]; then
    echo 'Mzzb Admin Not Running'
fi
