--liquibase formatted sql

--changeset nazar:006-fix-amount-scale
-- Змінюємо точність з 3 знаків на 2
ALTER TABLE invoice_items ALTER COLUMN amount DECIMAL(19, 2);

--changeset nazar:007-add-unit-price
ALTER TABLE invoice_items ADD unit_price NVARCHAR(20);

--rollback ALTER TABLE invoice_items DROP COLUMN unit_price;
--rollback ALTER TABLE invoice_items ALTER COLUMN amount DECIMAL(19, 3);

