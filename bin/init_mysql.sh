#!/usr/bin/env bash

# 环境准备
DirName=$(dirname "$0")
AppHome=$(realpath "$DirName"/..)

echo "Setup mysql database and user"
mysql -uroot -p"$DBPASS" <"$AppHome"/bin/init_mysql.sql
echo "Setup done"
