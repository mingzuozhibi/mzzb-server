rename table auto_login to remember;
rename table disc_group to `group`;
rename table disc_group_discs to group_discs;
alter table group_discs rename column disc_group_id to group_id;
