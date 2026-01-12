--liquibase formatted sql

--changeset nazar:001-initial-schema
--liquibase formatted sql

--changeset yura:002-create-invoice-items
CREATE TABLE invoice_items (
                               id BIGINT IDENTITY(1,1) PRIMARY KEY,

                               line_number INT,

    -- DECIMAL(19, 2) стандарт для грошей (до 19 цифр всього, 2 знаки після коми)
                               price DECIMAL(19, 2),

    -- invoice_number з Power Automate
                               invoice_number NVARCHAR(100),

                               vendor_tax_id NVARCHAR(50),

    -- raw_descr, ліміт 1000 символів, N для підтримки кирилиці
                               raw_descr NVARCHAR(1000)

    -- Якщо ви захочете реалізувати TODO в базі, розкоментуйте це:
    -- article NVARCHAR(50),
    -- item_name NVARCHAR(255)
);

--rollback DROP TABLE invoice_items;