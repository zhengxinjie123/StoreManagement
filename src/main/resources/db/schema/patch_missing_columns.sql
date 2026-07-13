-- Patch script for existing Store_Management databases.
-- Safe to run multiple times.

IF OBJECT_ID('dbo.operation_logs', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.operation_logs', 'action_type') IS NULL
        ALTER TABLE dbo.operation_logs ADD action_type NVARCHAR(30) NULL;
    IF COL_LENGTH('dbo.operation_logs', 'description') IS NULL
        ALTER TABLE dbo.operation_logs ADD description NVARCHAR(500) NULL;
    IF COL_LENGTH('dbo.operation_logs', 'request_body') IS NULL
        ALTER TABLE dbo.operation_logs ADD request_body NVARCHAR(MAX) NULL;
    IF COL_LENGTH('dbo.operation_logs', 'response_body') IS NULL
        ALTER TABLE dbo.operation_logs ADD response_body NVARCHAR(MAX) NULL;
END;

IF OBJECT_ID('dbo.import_attachments', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.import_attachments', 'last_import_error') IS NULL
        ALTER TABLE dbo.import_attachments ADD last_import_error NVARCHAR(500) NULL;
END;

IF OBJECT_ID('dbo.invoice_archives', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.invoice_archives', 'filtered_amount') IS NULL
        ALTER TABLE dbo.invoice_archives ADD filtered_amount DECIMAL(18, 4) NULL;
END;

IF OBJECT_ID('dbo.invoice_templates', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.invoice_templates', 'active') IS NULL
        ALTER TABLE dbo.invoice_templates ADD active BIT NOT NULL CONSTRAINT DF_invoice_templates_active DEFAULT 1;
    IF COL_LENGTH('dbo.invoice_templates', 'last_used_at') IS NULL
        ALTER TABLE dbo.invoice_templates ADD last_used_at DATETIME2 NULL;
END;

IF OBJECT_ID('dbo.retail_price_sync_runs', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.retail_price_sync_runs', 'rollback_of_run_id') IS NULL
        ALTER TABLE dbo.retail_price_sync_runs ADD rollback_of_run_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.retail_price_sync_run_lines', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.retail_price_sync_run_lines (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        run_id BIGINT NOT NULL,
        product_guid NVARCHAR(36) NOT NULL,
        barcode NVARCHAR(100) NOT NULL,
        old_retail_price_tax DECIMAL(18, 4) NULL,
        new_retail_price_tax DECIMAL(18, 4) NOT NULL,
        source_code NVARCHAR(50) NULL,
        source_name NVARCHAR(200) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_retail_price_sync_run_lines_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_retail_price_sync_run_lines_run FOREIGN KEY (run_id) REFERENCES retail_price_sync_runs(id) ON DELETE CASCADE
    );
    CREATE INDEX IX_retail_price_sync_run_lines_run ON dbo.retail_price_sync_run_lines(run_id);
    CREATE INDEX IX_retail_price_sync_run_lines_barcode ON dbo.retail_price_sync_run_lines(barcode);
END;

IF OBJECT_ID('dbo.retail_price_manual_changes', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.retail_price_manual_changes (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        product_guid NVARCHAR(36) NOT NULL,
        barcode NVARCHAR(100) NOT NULL,
        product_name NVARCHAR(200) NULL,
        supplier_name NVARCHAR(200) NULL,
        old_retail_price_tax DECIMAL(18, 4) NULL,
        new_retail_price_tax DECIMAL(18, 4) NOT NULL,
        username NVARCHAR(80) NULL,
        rolled_back BIT NOT NULL CONSTRAINT DF_retail_price_manual_changes_rolled_back DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_retail_price_manual_changes_created_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_retail_price_manual_changes_created ON dbo.retail_price_manual_changes(created_at);
    CREATE INDEX IX_retail_price_manual_changes_barcode ON dbo.retail_price_manual_changes(barcode);
END;

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
