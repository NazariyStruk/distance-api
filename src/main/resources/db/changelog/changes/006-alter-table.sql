--liquibase formatted sql

--changeset nazar:008-add-weight-column
ALTER TABLE invoice_items ADD weight DECIMAL(19, 2);

--rollback ALTER TABLE invoice_items DROP COLUMN weight;