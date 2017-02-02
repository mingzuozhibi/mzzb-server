create database mzzb_pro charset utf8;
create user 'mzzb_pro'@'localhost' identified by 'mzzb_pro';
grant all privileges on mzzb_pro.* to 'mzzb_pro'@'localhost';

create database mzzb_dev charset utf8;
create user 'mzzb_dev'@'localhost' identified by 'mzzb_dev';
grant all privileges on mzzb_dev.* to 'mzzb_dev'@'localhost';

flush privileges;
