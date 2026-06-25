IF OBJECT_ID('dbo.app_config', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.app_config (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        parent_id BIGINT NULL,
        config_key NVARCHAR(160) NOT NULL,
        config_value NVARCHAR(MAX) NULL,
        value_type NVARCHAR(32) NOT NULL,
        label NVARCHAR(120) NOT NULL,
        description NVARCHAR(500) NULL,
        sort_order INT NOT NULL CONSTRAINT DF_app_config_sort_order DEFAULT 0,
        system_flag BIT NOT NULL CONSTRAINT DF_app_config_system_flag DEFAULT 0,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );

    CREATE UNIQUE INDEX UX_app_config_config_key ON dbo.app_config(config_key);
    CREATE INDEX IX_app_config_parent_id ON dbo.app_config(parent_id);
END;

DECLARE @now DATETIME2 = SYSDATETIME();

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'security')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'security', NULL, 'GROUP', N'安全配置', N'系统安全与加密相关配置', 10, 1, @now, @now);
DECLARE @security BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'security');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'security.datasource-config-key')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@security, 'security.datasource-config-key', 'store-management-local-key', 'PASSWORD', N'数据源配置加密 Key', NULL, 10, 1, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'google-drive', NULL, 'GROUP', N'Google Drive 配置', N'父母发票上传云端配置', 20, 1, @now, @now);
DECLARE @google BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'google-drive');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive.enabled')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@google, 'google-drive.enabled', 'true', 'BOOLEAN', N'是否启用', NULL, 10, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive.client-id')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@google, 'google-drive.client-id', '', 'STRING', N'Client ID', NULL, 20, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive.client-secret')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@google, 'google-drive.client-secret', '', 'PASSWORD', N'Client Secret', NULL, 30, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive.token-path')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@google, 'google-drive.token-path', 'C:/Users/z1286/Desktop/Scan-delivery/token.json', 'STRING', N'Token 路径', NULL, 40, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive.folder-name')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@google, 'google-drive.folder-name', 'Fatura', 'STRING', N'文件夹名称', NULL, 50, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'google-drive.folder-id')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@google, 'google-drive.folder-id', '', 'STRING', N'文件夹 ID', NULL, 60, 1, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'upload')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'upload', NULL, 'GROUP', N'上传路径配置', N'附件与归档文件的本地存储路径', 30, 1, @now, @now);
DECLARE @upload BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'upload');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'upload.attachment-dir')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@upload, 'upload.attachment-dir', 'uploads/import-attachments', 'STRING', N'附件目录', NULL, 10, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'upload.invoice-archive-dir')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@upload, 'upload.invoice-archive-dir', 'uploads/invoice-archives', 'STRING', N'归档目录', NULL, 20, 1, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'invoice')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'invoice', NULL, 'GROUP', N'发票配置', N'发票清洗默认配置', 40, 1, @now, @now);
DECLARE @invoice BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'invoice');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'invoice.default-tax-rate')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@invoice, 'invoice.default-tax-rate', '23', 'DECIMAL', N'默认税率', NULL, 10, 1, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'archive')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'archive', NULL, 'GROUP', N'归档配置', N'归档文件命名配置', 50, 1, @now, @now);
DECLARE @archive BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'archive');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'archive.max-name-suffix-attempts')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@archive, 'archive.max-name-suffix-attempts', '1000', 'INTEGER', N'最大重名尝试次数', NULL, 10, 1, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'product', NULL, 'GROUP', N'TALENT 商品默认值', N'采购导入新商品默认 GUID', 60, 1, @now, @now);
DECLARE @product BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'product');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.type-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.type-guid', '57c8854a-b1d2-4fa0-b5a0-057b547a3e97', 'STRING', N'商品类型 GUID', NULL, 10, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.supplier-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.supplier-guid', '4cd95e7a-a422-4334-a82f-678ebf6f5363', 'STRING', N'默认供应商 GUID', NULL, 20, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.product-unit-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.product-unit-guid', 'f4bf65c4-18d9-49d3-ba03-734d1500e80d', 'STRING', N'商品单位 GUID', NULL, 30, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.depot-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.depot-guid', 'c3308645-2dd7-4ddd-8596-3bf0cb63c88f', 'STRING', N'仓库 GUID', NULL, 40, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.label-style-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.label-style-guid', '6E7985B0-5A58-4411-A3AB-4F867E201DC9', 'STRING', N'标签样式 GUID', NULL, 50, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.product-label-style-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.product-label-style-guid', 'DF53D3B7-19C6-467D-8A10-64749525A047', 'STRING', N'商品标签样式 GUID', NULL, 60, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'product.batch-no')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@product, 'product.batch-no', 'SimpleBatch', 'STRING', N'批次号', NULL, 70, 1, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (NULL, 'purchase', NULL, 'GROUP', N'TALENT 采购默认值', N'采购入库默认用户与供应商类型', 70, 1, @now, @now);
DECLARE @purchase BIGINT = (SELECT id FROM dbo.app_config WHERE config_key = 'purchase');
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.employee-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.employee-guid', 'ad4a220a-1e7a-4359-b680-6e036f56d811', 'STRING', N'员工 GUID', NULL, 10, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.marker-user-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.marker-user-guid', '5dbf3933-1cf4-4d3c-bef0-27b3b56f9697', 'STRING', N'制单人 GUID', NULL, 20, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.approver-user-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.approver-user-guid', '5dbf3933-1cf4-4d3c-bef0-27b3b56f9697', 'STRING', N'审批人 GUID', NULL, 30, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.product-unit-name')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.product-unit-name', 'p', 'STRING', N'商品单位名称', NULL, 40, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.chinese-supplier-type-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.chinese-supplier-type-guid', 'EA924E2E-7936-4D8C-841E-0F7E9A445DE3', 'STRING', N'中文供应商类型 GUID', NULL, 50, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.foreign-supplier-type-guid')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.foreign-supplier-type-guid', '25AFFF2D-A59A-4582-B98E-BE545DB916A5', 'STRING', N'外文供应商类型 GUID', NULL, 60, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.chinese-supplier-iso-country-code')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.chinese-supplier-iso-country-code', 'CN', 'STRING', N'中文供应商国家', NULL, 70, 1, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.app_config WHERE config_key = 'purchase.foreign-supplier-iso-country-code')
    INSERT INTO dbo.app_config(parent_id, config_key, config_value, value_type, label, description, sort_order, system_flag, created_at, updated_at)
    VALUES (@purchase, 'purchase.foreign-supplier-iso-country-code', 'PT', 'STRING', N'外文供应商国家', NULL, 80, 1, @now, @now);
