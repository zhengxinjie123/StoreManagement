package com.joao.storemanagement.serviceImpl.talent;

import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.service.talent.TalentPurchaseImportService;
import com.joao.storemanagement.talent.purchase.dto.PurchaseImportResult;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseDetailMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseHeaderMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseInventoryMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseParameterMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseProductMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseProductTypeMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseSupplierMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseSupplierTypeMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseTaxMapper;
import com.joao.storemanagement.talent.purchase.service.PurchaseImportEngine;
import com.joao.storemanagement.talent.purchase.service.PurchaseProductCodeService;
import com.joao.storemanagement.talent.purchase.service.PurchaseSupplierService;
import com.joao.storemanagement.talent.purchase.service.PurchaseTaxRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TalentPurchaseImportServiceImpl implements TalentPurchaseImportService {

    private final TalentOposDataSourceService talentOposDataSourceService;
    private final StoreProperties storeProperties;

    @Override
    public PurchaseImportResult importFromExcel(MultipartFile file) {
        return executeWithEngine(engine -> engine.importFromExcel(file));
    }

    @Override
    public PurchaseImportResult importFromArchive(Path archivePath, String fileName, String supplierGuid, String invoiceNo) {
        return importFromArchive(archivePath, fileName, supplierGuid, invoiceNo, null);
    }

    @Override
    public PurchaseImportResult importFromArchive(
            Path archivePath, String fileName, String supplierGuid, String invoiceNo, Boolean templateTaxIncluded) {
        return executeWithEngine(engine ->
                engine.importFromArchive(archivePath, supplierGuid, invoiceNo, templateTaxIncluded));
    }

    private PurchaseImportResult executeWithEngine(Function<PurchaseImportEngine, PurchaseImportResult> action) {
        return talentOposDataSourceService.executeInWriteTransaction(session -> {
            PurchaseProductMapper productMapper = session.getMapper(PurchaseProductMapper.class);
            PurchaseInventoryMapper inventoryMapper = session.getMapper(PurchaseInventoryMapper.class);
            PurchaseHeaderMapper purchaseHeaderMapper = session.getMapper(PurchaseHeaderMapper.class);
            PurchaseDetailMapper purchaseDetailMapper = session.getMapper(PurchaseDetailMapper.class);
            PurchaseParameterMapper parameterMapper = session.getMapper(PurchaseParameterMapper.class);
            PurchaseProductTypeMapper productTypeMapper = session.getMapper(PurchaseProductTypeMapper.class);
            PurchaseSupplierMapper supplierMapper = session.getMapper(PurchaseSupplierMapper.class);
            PurchaseSupplierTypeMapper supplierTypeMapper = session.getMapper(PurchaseSupplierTypeMapper.class);
            PurchaseTaxMapper taxMapper = session.getMapper(PurchaseTaxMapper.class);

            PurchaseProductCodeService productCodeService =
                    new PurchaseProductCodeService(productMapper, productTypeMapper, parameterMapper);
            PurchaseSupplierService supplierService =
                    new PurchaseSupplierService(supplierMapper, supplierTypeMapper, parameterMapper, storeProperties);
            PurchaseTaxRateService taxRateService = new PurchaseTaxRateService(taxMapper);

            PurchaseImportEngine engine = new PurchaseImportEngine(
                    productMapper,
                    inventoryMapper,
                    purchaseHeaderMapper,
                    purchaseDetailMapper,
                    parameterMapper,
                    productCodeService,
                    supplierService,
                    taxRateService,
                    storeProperties);
            return action.apply(engine);
        });
    }
}
