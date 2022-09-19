RENAME TABLE `auto_login` TO `remember`;
RENAME TABLE `disc_group` TO `group`;
RENAME TABLE `disc_group_discs` TO `group_discs`;
ALTER TABLE `group_discs` RENAME COLUMN `disc_group_id` TO `group_id`;
