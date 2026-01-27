--liquibase formatted sql

--changeset nazar:006-add-financial-amount
ALTER TABLE invoice_items ADD quantity DECIMAL(19, 2);

--rollback ALTER TABLE invoice_items DROP COLUMN amount;