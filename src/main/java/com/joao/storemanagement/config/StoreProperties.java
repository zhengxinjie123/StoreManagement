package com.joao.storemanagement.config;

import com.joao.storemanagement.service.primary.AppConfigService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class StoreProperties {

    private final AppConfigService appConfigService;

    public Invoice getInvoice() {
        Invoice invoice = new Invoice();
        invoice.setDefaultTaxRate(appConfigService.getBigDecimal(
                "invoice.default-tax-rate", new BigDecimal("23")));
        return invoice;
    }

    public Archive getArchive() {
        Archive archive = new Archive();
        archive.setMaxNameSuffixAttempts(appConfigService.getInteger(
                "archive.max-name-suffix-attempts", 1000));
        return archive;
    }

    public GoogleDrive getGoogleDrive() {
        GoogleDrive googleDrive = new GoogleDrive();
        googleDrive.setEnabled(appConfigService.getBoolean("google-drive.enabled", true));
        googleDrive.setClientId(appConfigService.getString("google-drive.client-id", ""));
        googleDrive.setClientSecret(appConfigService.getString("google-drive.client-secret", ""));
        googleDrive.setTokenPath(appConfigService.getString(
                "google-drive.token-path", "C:/Users/z1286/Desktop/Scan-delivery/token.json"));
        googleDrive.setFolderName(appConfigService.getString("google-drive.folder-name", "Fatura"));
        googleDrive.setFolderId(appConfigService.getString("google-drive.folder-id", ""));
        return googleDrive;
    }

    public ProductDefaults getProduct() {
        ProductDefaults product = new ProductDefaults();
        product.setTypeGuid(appConfigService.getString(
                "product.type-guid", "57c8854a-b1d2-4fa0-b5a0-057b547a3e97"));
        product.setSupplierGuid(appConfigService.getString(
                "product.supplier-guid", "4cd95e7a-a422-4334-a82f-678ebf6f5363"));
        product.setProductUnitGuid(appConfigService.getString(
                "product.product-unit-guid", "f4bf65c4-18d9-49d3-ba03-734d1500e80d"));
        product.setDepotGuid(appConfigService.getString(
                "product.depot-guid", "c3308645-2dd7-4ddd-8596-3bf0cb63c88f"));
        product.setLabelStyleGuid(appConfigService.getString(
                "product.label-style-guid", "6E7985B0-5A58-4411-A3AB-4F867E201DC9"));
        product.setProductLabelStyleGuid(appConfigService.getString(
                "product.product-label-style-guid", "DF53D3B7-19C6-467D-8A10-64749525A047"));
        product.setBatchNo(appConfigService.getString("product.batch-no", "SimpleBatch"));
        return product;
    }

    public PurchaseDefaults getPurchase() {
        PurchaseDefaults purchase = new PurchaseDefaults();
        purchase.setEmployeeGuid(appConfigService.getString(
                "purchase.employee-guid", "ad4a220a-1e7a-4359-b680-6e036f56d811"));
        purchase.setMarkerUserGuid(appConfigService.getString(
                "purchase.marker-user-guid", "5dbf3933-1cf4-4d3c-bef0-27b3b56f9697"));
        purchase.setApproverUserGuid(appConfigService.getString(
                "purchase.approver-user-guid", "5dbf3933-1cf4-4d3c-bef0-27b3b56f9697"));
        purchase.setProductUnitName(appConfigService.getString("purchase.product-unit-name", "p"));
        purchase.setChineseSupplierTypeGuid(appConfigService.getString(
                "purchase.chinese-supplier-type-guid", "EA924E2E-7936-4D8C-841E-0F7E9A445DE3"));
        purchase.setForeignSupplierTypeGuid(appConfigService.getString(
                "purchase.foreign-supplier-type-guid", "25AFFF2D-A59A-4582-B98E-BE545DB916A5"));
        purchase.setChineseSupplierIsoCountryCode(appConfigService.getString(
                "purchase.chinese-supplier-iso-country-code", "CN"));
        purchase.setForeignSupplierIsoCountryCode(appConfigService.getString(
                "purchase.foreign-supplier-iso-country-code", "PT"));
        return purchase;
    }

    public Upload getUpload() {
        Upload upload = new Upload();
        upload.setAttachmentDir(appConfigService.getString(
                "upload.attachment-dir", "uploads/import-attachments"));
        upload.setInvoiceArchiveDir(appConfigService.getString(
                "upload.invoice-archive-dir", "uploads/invoice-archives"));
        return upload;
    }

    @Getter
    @Setter
    public static class Invoice {
        private BigDecimal defaultTaxRate = new BigDecimal("23");
    }

    @Getter
    @Setter
    public static class Archive {
        private int maxNameSuffixAttempts = 1000;
    }

    @Getter
    @Setter
    public static class GoogleDrive {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
        private String tokenPath;
        private String folderName = "Fatura";
        private String folderId;
    }

    @Getter
    @Setter
    public static class Upload {
        private String attachmentDir = "uploads/import-attachments";
        private String invoiceArchiveDir = "uploads/invoice-archives";
    }

    @Getter
    @Setter
    public static class ProductDefaults {
        private String typeGuid = "57c8854a-b1d2-4fa0-b5a0-057b547a3e97";
        private String supplierGuid = "4cd95e7a-a422-4334-a82f-678ebf6f5363";
        private String productUnitGuid = "f4bf65c4-18d9-49d3-ba03-734d1500e80d";
        private String depotGuid = "c3308645-2dd7-4ddd-8596-3bf0cb63c88f";
        private String labelStyleGuid = "6E7985B0-5A58-4411-A3AB-4F867E201DC9";
        private String productLabelStyleGuid = "DF53D3B7-19C6-467D-8A10-64749525A047";
        private String batchNo = "SimpleBatch";
    }

    @Getter
    @Setter
    public static class PurchaseDefaults {
        private String employeeGuid = "ad4a220a-1e7a-4359-b680-6e036f56d811";
        private String markerUserGuid = "5dbf3933-1cf4-4d3c-bef0-27b3b56f9697";
        private String approverUserGuid = "5dbf3933-1cf4-4d3c-bef0-27b3b56f9697";
        private String productUnitName = "p";
        private String chineseSupplierTypeGuid = "EA924E2E-7936-4D8C-841E-0F7E9A445DE3";
        private String foreignSupplierTypeGuid = "25AFFF2D-A59A-4582-B98E-BE545DB916A5";
        private String chineseSupplierIsoCountryCode = "CN";
        private String foreignSupplierIsoCountryCode = "PT";
    }
}
