-- Finance, pricing, replenish module tables for Store_Management

-- Revenue / expense (from MeuERP V1)
IF OBJECT_ID('dbo.revenue_entries', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.revenue_entries (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        entry_date DATE NOT NULL,
        amount DECIMAL(18, 2) NOT NULL,
        channel NVARCHAR(100) NULL,
        remark NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_revenue_entries_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_revenue_entries_updated_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_revenue_entries_entry_date ON dbo.revenue_entries(entry_date);
END;

IF OBJECT_ID('dbo.expense_entries', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.expense_entries (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        entry_date DATE NOT NULL,
        amount DECIMAL(18, 2) NOT NULL,
        category NVARCHAR(100) NOT NULL,
        is_proxy_payment BIT NOT NULL CONSTRAINT DF_expense_entries_is_proxy DEFAULT 0,
        remark NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_expense_entries_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_expense_entries_updated_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_expense_entries_entry_date ON dbo.expense_entries(entry_date);
END;

-- Supplier payment pending (V7 + V10 + V11)
IF OBJECT_ID('dbo.supplier_payment_pending', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.supplier_payment_pending (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        supplier_name NVARCHAR(200) NOT NULL,
        order_date DATE NOT NULL,
        arrival_date DATE NULL,
        payment_due_date DATE NULL,
        amount DECIMAL(18, 2) NULL,
        remark NVARCHAR(500) NULL,
        status NVARCHAR(30) NOT NULL CONSTRAINT DF_supplier_payment_pending_status DEFAULT N'open',
        paid_date DATE NULL,
        payment_method NVARCHAR(30) NULL,
        payment_term NVARCHAR(50) NULL,
        payment_term_code NVARCHAR(30) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_supplier_payment_pending_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_supplier_payment_pending_updated_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_supplier_payment_pending_due ON dbo.supplier_payment_pending(payment_due_date);
END;

IF OBJECT_ID('dbo.supplier_payment_pending', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.supplier_payment_pending', 'payment_method') IS NULL
        ALTER TABLE dbo.supplier_payment_pending ADD payment_method NVARCHAR(30) NULL;
    IF COL_LENGTH('dbo.supplier_payment_pending', 'payment_term') IS NULL
        ALTER TABLE dbo.supplier_payment_pending ADD payment_term NVARCHAR(50) NULL;
    IF COL_LENGTH('dbo.supplier_payment_pending', 'payment_term_code') IS NULL
        ALTER TABLE dbo.supplier_payment_pending ADD payment_term_code NVARCHAR(30) NULL;
    IF COL_LENGTH('dbo.supplier_payment_pending', 'advance_payment_date') IS NOT NULL
        ALTER TABLE dbo.supplier_payment_pending DROP COLUMN advance_payment_date;
END;

IF OBJECT_ID('dbo.payment_due_terms', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_due_terms (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(30) NOT NULL,
        label NVARCHAR(50) NOT NULL,
        offset_days INT NULL,
        offset_months INT NULL,
        unknown_term BIT NOT NULL CONSTRAINT DF_payment_due_terms_unknown DEFAULT 0,
        sort_order INT NOT NULL CONSTRAINT DF_payment_due_terms_sort DEFAULT 0,
        active BIT NOT NULL CONSTRAINT DF_payment_due_terms_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_payment_due_terms_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_payment_due_terms_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_payment_due_terms_code UNIQUE (code)
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.payment_due_terms WHERE code = N'7d')
    INSERT INTO dbo.payment_due_terms (code, label, offset_days, offset_months, unknown_term, sort_order)
    VALUES (N'7d', N'7天内', 7, NULL, 0, 10);
IF NOT EXISTS (SELECT 1 FROM dbo.payment_due_terms WHERE code = N'14d')
    INSERT INTO dbo.payment_due_terms (code, label, offset_days, offset_months, unknown_term, sort_order)
    VALUES (N'14d', N'14天内', 14, NULL, 0, 20);
IF NOT EXISTS (SELECT 1 FROM dbo.payment_due_terms WHERE code = N'1m')
    INSERT INTO dbo.payment_due_terms (code, label, offset_days, offset_months, unknown_term, sort_order)
    VALUES (N'1m', N'1个月', NULL, 1, 0, 30);
IF NOT EXISTS (SELECT 1 FROM dbo.payment_due_terms WHERE code = N'2m')
    INSERT INTO dbo.payment_due_terms (code, label, offset_days, offset_months, unknown_term, sort_order)
    VALUES (N'2m', N'2个月', NULL, 2, 0, 40);
IF NOT EXISTS (SELECT 1 FROM dbo.payment_due_terms WHERE code = N'3m')
    INSERT INTO dbo.payment_due_terms (code, label, offset_days, offset_months, unknown_term, sort_order)
    VALUES (N'3m', N'3个月', NULL, 3, 0, 50);
IF NOT EXISTS (SELECT 1 FROM dbo.payment_due_terms WHERE code = N'unknown')
    INSERT INTO dbo.payment_due_terms (code, label, offset_days, offset_months, unknown_term, sort_order)
    VALUES (N'unknown', N'未知', NULL, NULL, 1, 99);

-- Replenish records (V7 + V9)
IF OBJECT_ID('dbo.replenish_records', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.replenish_records (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        barcode NVARCHAR(100) NOT NULL,
        name_chinese NVARCHAR(300) NULL,
        name_foreign NVARCHAR(300) NULL,
        supplier_name NVARCHAR(200) NOT NULL,
        inventory_qty DECIMAL(18, 2) NULL,
        remark NVARCHAR(500) NULL,
        status NVARCHAR(30) NOT NULL CONSTRAINT DF_replenish_records_status DEFAULT N'pending',
        created_at DATETIME2 NOT NULL CONSTRAINT DF_replenish_records_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_replenish_records_updated_at DEFAULT SYSUTCDATETIME()
    );
    CREATE INDEX IX_replenish_records_supplier ON dbo.replenish_records(supplier_name);
END;

IF OBJECT_ID('dbo.replenish_records', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.replenish_records', 'name_chinese') IS NULL
        ALTER TABLE dbo.replenish_records ADD name_chinese NVARCHAR(300) NULL;
    IF COL_LENGTH('dbo.replenish_records', 'name_foreign') IS NULL
        ALTER TABLE dbo.replenish_records ADD name_foreign NVARCHAR(300) NULL;
    IF COL_LENGTH('dbo.replenish_records', 'inventory_qty') IS NULL
        ALTER TABLE dbo.replenish_records ADD inventory_qty DECIMAL(18, 2) NULL;
END;

-- Retail price reference (V8 + V12 + V13)
IF OBJECT_ID('dbo.retail_price_sources', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.retail_price_sources (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(50) NOT NULL,
        name NVARCHAR(200) NOT NULL,
        priority INT NOT NULL CONSTRAINT DF_retail_price_sources_priority DEFAULT 100,
        remark NVARCHAR(500) NULL,
        active BIT NOT NULL CONSTRAINT DF_retail_price_sources_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_retail_price_sources_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_retail_price_sources_updated_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_retail_price_sources_code UNIQUE (code)
    );
END;

IF OBJECT_ID('dbo.retail_price_ref_lines', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.retail_price_ref_lines (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        source_id BIGINT NOT NULL,
        barcode NVARCHAR(100) NOT NULL,
        product_name NVARCHAR(2000) NULL,
        retail_price DECIMAL(18, 4) NOT NULL,
        purchase_price_tax DECIMAL(18, 4) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_retail_price_ref_lines_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_retail_price_ref_lines_source FOREIGN KEY (source_id) REFERENCES retail_price_sources(id) ON DELETE CASCADE,
        CONSTRAINT UQ_retail_price_ref_lines_source_barcode UNIQUE (source_id, barcode)
    );
    CREATE INDEX IX_retail_price_ref_lines_barcode ON dbo.retail_price_ref_lines(barcode);
END;

IF OBJECT_ID('dbo.retail_price_ref_lines', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.retail_price_ref_lines', 'purchase_price_tax') IS NULL
        ALTER TABLE dbo.retail_price_ref_lines ADD purchase_price_tax DECIMAL(18, 4) NULL;
END;

IF OBJECT_ID('dbo.retail_price_sync_runs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.retail_price_sync_runs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        matched_count INT NOT NULL,
        applied_count INT NOT NULL,
        skipped_count INT NOT NULL,
        remark NVARCHAR(500) NULL,
        rollback_of_run_id BIGINT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_retail_price_sync_runs_created_at DEFAULT SYSUTCDATETIME()
    );
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

IF NOT EXISTS (SELECT 1 FROM dbo.retail_price_sources WHERE code = N'STORE_A')
    INSERT INTO dbo.retail_price_sources (code, name, priority, remark, active)
    VALUES (N'STORE_A', N'店家A', 1, N'优先参考售价', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.retail_price_sources WHERE code = N'STORE_B')
    INSERT INTO dbo.retail_price_sources (code, name, priority, remark, active)
    VALUES (N'STORE_B', N'店家B', 2, N'次优先参考售价', 1);
