#!/usr/bin/env bash
basepath=/home/ubuntu/record
backroot=${basepath}/nginx
backfile=${backroot}/access.log

# 存放每天日志
datetext=$(date +%Y%m%d)
copypath=${backroot}/backup
mkdir -p ${copypath}

mv ${backfile} ${copypath}/access-${datetext}.log
kill -USR1 $(cat /var/run/nginx.pid)

# 清理超过30天的日志
find ${copypath} -type f -mtime +30 | xargs rm
