--changeset yuriy:006-add-created-at-to-nakladna
ALTER TABLE nakladna ADD created_at DATETIME DEFAULT GETDATE();