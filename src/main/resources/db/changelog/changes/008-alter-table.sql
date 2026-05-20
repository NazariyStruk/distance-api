--liquibase formatted sql

--changeset yuriy:005-add-file-name-to-acts
ALTER TABLE acts
    ADD file_name NVARCHAR(255);

--rollback ALTER TABLE acts DROP COLUMN file_name;