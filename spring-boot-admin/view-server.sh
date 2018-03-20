#!/usr/bin/env bash

pid=$(ps -x | grep 'target.name=mzzb-admin' | grep -v grep | awk '{print $1}')

if [ -n "${pid}" ]; then
    echo 'Mzzb Admin Pid:' ${pid}
fi

if [ -z "${pid}" ]; then
    echo 'Mzzb Admin Not Running'
fi
