ALTER TABLE sakura_discs RENAME disc_group_discs;
ALTER TABLE sakura RENAME disc_group;

ALTER TABLE disc_group_discs
    DROP FOREIGN KEY FKc1blr2smynys592tww1xbrn8h;
ALTER TABLE disc_group_discs
    DROP FOREIGN KEY FKewfigbo8cxuw8rykhdlvuhd90;

ALTER TABLE disc_group_discs
    CHANGE COLUMN sakura_id disc_group_id BIGINT NOT NULL;