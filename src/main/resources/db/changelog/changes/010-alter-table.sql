--liquibase formatted sql

--changeset yuriy:005-create-nakladna-tables
CREATE TABLE nakladna (
                          id BIGINT IDENTITY(1,1) PRIMARY KEY,
                          document_type NVARCHAR(50),
                          invoice_id NVARCHAR(50),
                          invoice_date NVARCHAR(50),
                          vendor_name NVARCHAR(255),
                          vendor_edrpou NVARCHAR(50),
                          vendor_ipn NVARCHAR(50),
                          total_amount DECIMAL(19, 2),
                          uploaded_to_1c BIT DEFAULT 0,
                          file_name NVARCHAR(255)
);

CREATE TABLE nakladna_items (
                                id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                nakladna_id BIGINT NOT NULL,
                                description NVARCHAR(MAX),
                                quantity DECIMAL(19, 3),
                                unit NVARCHAR(20),
                                unit_price DECIMAL(19, 2),
                                amount DECIMAL(19, 2),
                                CONSTRAINT fk_nakladna_items_nakladna FOREIGN KEY (nakladna_id) REFERENCES nakladna (id) ON DELETE CASCADE
);

CREATE INDEX idx_nakladna_items_nakladna_id ON nakladna_items (nakladna_id);

--rollback DROP TABLE nakladna_items;
--rollback DROP TABLE nakladna;