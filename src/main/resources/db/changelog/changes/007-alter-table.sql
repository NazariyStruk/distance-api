--liquibase formatted sql

--changeset yuriy:004-create-acts-tables
CREATE TABLE acts (
                      id BIGINT IDENTITY(1,1) PRIMARY KEY,
                      number_doc NVARCHAR(50),
                      date_doc NVARCHAR(50),
                      type_doc NVARCHAR(50),
                      name_supplier NVARCHAR(255),
                      code_supplier NVARCHAR(50),
                      ipn_supplier NVARCHAR(50),
                      amount_doc DECIMAL(19, 2),
                      tax_doc DECIMAL(19, 2),
                      include_tax NVARCHAR(50)
);

CREATE TABLE act_items (
                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           act_id BIGINT NOT NULL,
                           articul NVARCHAR(50),
                           description NVARCHAR(MAX),
                           quantity INT,
                           units NVARCHAR(20),
                           price DECIMAL(19, 2),
                           amount DECIMAL(19, 2),
                           tax DECIMAL(19, 2),
                           CONSTRAINT fk_act_items_act FOREIGN KEY (act_id) REFERENCES acts (id) ON DELETE CASCADE
);

-- Індекс для прискорення вибірки позицій конкретного акту
CREATE INDEX idx_act_items_act_id ON act_items (act_id);

--rollback DROP TABLE act_items;
--rollback DROP TABLE acts;