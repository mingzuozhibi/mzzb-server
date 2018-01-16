#!/usr/bin/env bash
URL=$1
shift

echo ''
echo 'http --session ./session.json :9090/api/'${URL} $@
echo ''

cd $(cd `dirname $0`; pwd)
http -v --session ./session.json :9090/api/${URL} $@
