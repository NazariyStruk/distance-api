--liquibase formatted sql

--changeset nazar:002-alter-table.sql
--liquibase formatted sql

--changeset nazar:003-add-extra-fields
ALTER TABLE invoice_items ADD articul NVARCHAR(50);
ALTER TABLE invoice_items ADD date DATE;
ALTER TABLE invoice_items ADD updated_by_1c BIT DEFAULT 0;

--rollback ALTER TABLE invoice_items DROP COLUMN articul;
--rollback ALTER TABLE invoice_items DROP COLUMN date;
--rollback ALTER TABLE invoice_items DROP COLUMN updated_by_1c;