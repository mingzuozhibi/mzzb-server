#!/usr/bin/env bash

echo "Sync remote db to local db"
ssh q 'bash admin/manual/gzip_mzzb_server.sh' |
    gzip -d | mysql -uroot -p$DB_PASS mzzb_server
echo "Sync done"
