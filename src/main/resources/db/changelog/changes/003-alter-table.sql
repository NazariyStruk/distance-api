--liquibase formatted sql

--changeset nazar:004-add-amount-units
ALTER TABLE invoice_items ADD amount DECIMAL(19, 3);
ALTER TABLE invoice_items ADD units NVARCHAR(50);

--rollback ALTER TABLE invoice_items DROP COLUMN amount;
--rollback ALTER TABLE invoice_items DROP COLUMN units;