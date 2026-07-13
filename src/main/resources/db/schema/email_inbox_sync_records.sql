IF OBJECT_ID('dbo.email_inbox_sync_records', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.email_inbox_sync_records (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        session_id NVARCHAR(64) NOT NULL,
        user_id BIGINT NOT NULL,
        operator_name NVARCHAR(80) NULL,
        sync_at DATETIME2 NOT NULL,
        range_from_date DATE NOT NULL,
        range_to_date DATE NOT NULL,
        scanned_messages INT NOT NULL CONSTRAINT DF_email_inbox_sync_records_scanned DEFAULT 0,
        attachment_count INT NOT NULL CONSTRAINT DF_email_inbox_sync_records_attachments DEFAULT 0,
        uploaded_count INT NOT NULL CONSTRAINT DF_email_inbox_sync_records_uploaded DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_email_inbox_sync_records_created_at DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_email_inbox_sync_records_sync_at ON dbo.email_inbox_sync_records(sync_at DESC);
    CREATE INDEX IX_email_inbox_sync_records_user ON dbo.email_inbox_sync_records(user_id);
END;
