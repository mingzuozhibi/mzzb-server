#!/usr/bin/env bash

if [[ $# == 1 && $1 == "-p" ]]; then
    ssh q "sh admin/gzip_mzzb_server.sh" | gzip -d | mysql -uroot -pfuhaiwei mzzb_pro
else
    ssh q "sh admin/gzip_mzzb_server.sh" | gzip -d | mysql -uroot -pfuhaiwei mzzb_dev
fi