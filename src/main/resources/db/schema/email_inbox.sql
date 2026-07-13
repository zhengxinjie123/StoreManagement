IF OBJECT_ID('dbo.email_inbox_attachments', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.email_inbox_attachments (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        message_uid NVARCHAR(64) NOT NULL,
        message_subject NVARCHAR(500) NULL,
        from_address NVARCHAR(320) NULL,
        received_at DATETIME2 NULL,
        file_name NVARCHAR(260) NOT NULL,
        extension_name NVARCHAR(16) NOT NULL,
        file_size BIGINT NULL,
        stored_path NVARCHAR(500) NOT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_email_inbox_attachments_status DEFAULT 'PENDING',
        import_attachment_uuid NVARCHAR(36) NULL,
        fetched_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_email_inbox_attachments_created_at DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX UX_email_inbox_attachments_uid_file
        ON dbo.email_inbox_attachments(message_uid, file_name);
END;
