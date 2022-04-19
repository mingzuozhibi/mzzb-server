#!/usr/bin/env bash

echo "Sync remote db to local db"
ssh q "mysqldump -uroot -p""$DBPASS"" mzzb_server | gzip" |
    gzip -d | mysql -uroot -p"$DBPASS" mzzb_server
echo "Sync done"
