#!/usr/bin/env bash
basepath=/home/ubuntu/backup

# 创建最新备份
backroot=${basepath}/mzzb_pro
backfile=${backroot}/backup.sql
mysqldump -uroot -pfuhaiwei mzzb_pro > ${backfile}

# 存放每小时备份
datetext=$(date +%Y%m%d)
timetext=$(date +%H%M%S)
copypath=${backroot}/hour_backup/${datetext}
mkdir -p ${copypath}
cp ${backfile} ${copypath}/backup-${datetext}-${timetext}.sql

# 清理超过十天的每小时备份
find ${backroot}/hour_backup -type f -mtime +9 | xargs rm

# 存放每天备份
monthtext=$(date +%Y%m)
copypath=${backroot}/date_backup/${monthtext}
mkdir -p ${copypath}
cp ${backfile} ${copypath}/backup-${datetext}.sql

# 清理超过一百天的每天备份
find ${backroot}/date_backup -type f -mtime +99 | xargs rm

# 清理空文件夹
find ${backroot} -type d -empty | xargs rm -rf
