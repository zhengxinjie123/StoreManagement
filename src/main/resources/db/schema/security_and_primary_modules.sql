-- Security and primary workflow tables for Store_Management

IF OBJECT_ID('dbo.system_users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.system_users (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        username NVARCHAR(80) NOT NULL,
        password_hash NVARCHAR(500) NOT NULL,
        role NVARCHAR(30) NOT NULL,
        active BIT NOT NULL CONSTRAINT DF_system_users_active DEFAULT 1,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT UQ_system_users_username UNIQUE (username)
    );
END;

IF OBJECT_ID('dbo.system_user_sessions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.system_user_sessions (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        token NVARCHAR(80) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT UQ_system_user_sessions_token UNIQUE (token)
    );
    CREATE INDEX IX_system_user_sessions_user ON dbo.system_user_sessions(user_id);
    CREATE INDEX IX_system_user_sessions_expires ON dbo.system_user_sessions(expires_at);
END;

IF OBJECT_ID('dbo.operation_logs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.operation_logs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        username NVARCHAR(80) NULL,
        action_type NVARCHAR(30) NULL,
        description NVARCHAR(500) NULL,
        http_method NVARCHAR(10) NOT NULL,
        request_uri NVARCHAR(500) NOT NULL,
        status_code INT NOT NULL,
        duration_ms BIGINT NOT NULL,
        client_ip NVARCHAR(80) NULL,
        created_at DATETIME2 NOT NULL
    );
    CREATE INDEX IX_operation_logs_created ON dbo.operation_logs(created_at);
    CREATE INDEX IX_operation_logs_username ON dbo.operation_logs(username);
END;

IF OBJECT_ID('dbo.operation_logs', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.operation_logs', 'action_type') IS NULL
        ALTER TABLE dbo.operation_logs ADD action_type NVARCHAR(30) NULL;
    IF COL_LENGTH('dbo.operation_logs', 'description') IS NULL
        ALTER TABLE dbo.operation_logs ADD description NVARCHAR(500) NULL;
END;

IF OBJECT_ID('dbo.talent_datasource_config', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.talent_datasource_config (
        id BIGINT NOT NULL PRIMARY KEY,
        host NVARCHAR(200) NOT NULL,
        port INT NOT NULL,
        database_name NVARCHAR(160) NOT NULL,
        username NVARCHAR(160) NOT NULL,
        password NVARCHAR(MAX) NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
END;

IF OBJECT_ID('dbo.import_attachments', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.import_attachments (
        uuid NVARCHAR(64) NOT NULL PRIMARY KEY,
        file_name NVARCHAR(500) NOT NULL,
        file_size BIGINT NOT NULL,
        upload_date DATETIME2 NOT NULL,
        extension_name NVARCHAR(30) NOT NULL,
        supplier_guid NVARCHAR(64) NOT NULL,
        import_status INT NULL,
        owner_type INT NULL,
        clean_status INT NULL,
        last_import_error NVARCHAR(500) NULL
    );
    CREATE INDEX IX_import_attachments_supplier ON dbo.import_attachments(supplier_guid);
    CREATE INDEX IX_import_attachments_upload ON dbo.import_attachments(upload_date);
END;

IF OBJECT_ID('dbo.import_attachments', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.import_attachments', 'last_import_error') IS NULL
        ALTER TABLE dbo.import_attachments ADD last_import_error NVARCHAR(500) NULL;
END;

IF OBJECT_ID('dbo.invoice_templates', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.invoice_templates', 'active') IS NULL
        ALTER TABLE dbo.invoice_templates ADD active BIT NOT NULL CONSTRAINT DF_invoice_templates_active DEFAULT 1;
    IF COL_LENGTH('dbo.invoice_templates', 'last_used_at') IS NULL
        ALTER TABLE dbo.invoice_templates ADD last_used_at DATETIME2 NULL;
END;

IF OBJECT_ID('dbo.invoice_templates', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.invoice_templates (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        supplier_guid NVARCHAR(64) NOT NULL,
        name NVARCHAR(160) NOT NULL,
        header_row INT NOT NULL,
        data_start_row INT NOT NULL,
        sheet_name NVARCHAR(160) NULL,
        barcode_col NVARCHAR(20) NOT NULL,
        new_barcode_col NVARCHAR(20) NULL,
        foreign_name_col NVARCHAR(20) NOT NULL,
        chinese_name_col NVARCHAR(20) NULL,
        quantity_col NVARCHAR(20) NOT NULL,
        price_col NVARCHAR(20) NULL,
        price_tax_included_col NVARCHAR(20) NULL,
        tax_rate_col NVARCHAR(20) NULL,
        line_subtotal_col NVARCHAR(20) NULL,
        default_tax_rate DECIMAL(18, 4) NULL,
        tax_included BIT NOT NULL,
        filter_rows_without_barcode BIT NOT NULL,
        split_mixed_chinese_foreign_name BIT NOT NULL,
        strip_currency_from_price BIT NOT NULL,
        skip_zero_price_pallet_rows BIT NOT NULL,
        break_on_taxable_base BIT NOT NULL,
        product_has_new_barcode BIT NOT NULL,
        skip_barcode_not_ean13 BIT NOT NULL,
        footer_summary_mode NVARCHAR(50) NULL,
        remark NVARCHAR(500) NULL,
        active BIT NOT NULL CONSTRAINT DF_invoice_templates_active DEFAULT 1,
        last_used_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
    CREATE INDEX IX_invoice_templates_supplier ON dbo.invoice_templates(supplier_guid);
END;

IF OBJECT_ID('dbo.invoice_archives', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.invoice_archives (
        uuid NVARCHAR(64) NOT NULL PRIMARY KEY,
        attachment_uuid NVARCHAR(64) NULL,
        supplier_guid NVARCHAR(64) NOT NULL,
        file_name NVARCHAR(500) NOT NULL,
        extension_name NVARCHAR(30) NOT NULL,
        file_size BIGINT NOT NULL,
        file_path NVARCHAR(1000) NOT NULL,
        row_count INT NULL,
        total_quantity DECIMAL(18, 4) NULL,
        amount_before_discount DECIMAL(18, 4) NULL,
        discount_amount DECIMAL(18, 4) NULL,
        total_amount DECIMAL(18, 4) NULL,
        filtered_amount DECIMAL(18, 4) NULL,
        tax_included BIT NULL,
        remark NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL
    );
    CREATE INDEX IX_invoice_archives_supplier ON dbo.invoice_archives(supplier_guid);
    CREATE INDEX IX_invoice_archives_created ON dbo.invoice_archives(created_at);
END;
