package com.joao.storemanagement.service.talent;

import com.joao.storemanagement.talent.purchase.dto.PurchaseImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface TalentPurchaseImportService {

    PurchaseImportResult importFromExcel(MultipartFile file);

    PurchaseImportResult importFromArchive(Path archivePath, String fileName, String supplierGuid, String invoiceNo);

    PurchaseImportResult importFromArchive(
            Path archivePath, String fileName, String supplierGuid, String invoiceNo, Boolean templateTaxIncluded);
}
