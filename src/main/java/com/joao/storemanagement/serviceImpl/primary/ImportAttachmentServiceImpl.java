package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.enums.AttachmentExtension;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.ImportAttachmentMapper;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.InvoiceArchiveService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.vo.primary.AttachmentVO;
import com.joao.storemanagement.utils.SupplierFileNameMatcher;
import com.joao.storemanagement.vo.primary.BatchUploadItemVO;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.BatchUploadSupplierMatchVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportAttachmentServiceImpl implements ImportAttachmentService {

    private final TalentOposDataSourceService talentOposDataSourceService;
    private final ImportAttachmentMapper importAttachmentMapper;
    private final InvoiceArchiveService invoiceArchiveService;
    private final SupplierFileNameMatcher supplierFileNameMatcher;
    private final StoreProperties storeProperties;

    @Override
    public PageResponseVO<AttachmentVO> page(long current, long pageSize, String supplierGuid,
                                             AttachmentOwner ownerType, CleanStatus cleanStatus,
                                             ImportStatus importStatus, LocalDate fromUploadDate,
                                             LocalDate toUploadDate) {
        LambdaQueryWrapper<ImportAttachment> query = Wrappers.lambdaQuery(ImportAttachment.class)
                .eq(StrUtil.isNotBlank(supplierGuid), ImportAttachment::getSupplierGuid, supplierGuid)
                .eq(ownerType != null, ImportAttachment::getOwnerType, ownerType)
                .eq(cleanStatus != null, ImportAttachment::getCleanStatus, cleanStatus)
                .eq(importStatus != null, ImportAttachment::getImportStatus, importStatus)
                .ge(fromUploadDate != null, ImportAttachment::getUploadDate,
                        fromUploadDate == null ? null : fromUploadDate.atStartOfDay())
                .lt(toUploadDate != null, ImportAttachment::getUploadDate,
                        toUploadDate == null ? null : toUploadDate.plusDays(1).atStartOfDay())
                .orderByDesc(ImportAttachment::getUploadDate);

        Page<ImportAttachment> page = importAttachmentMapper.selectPage(Page.of(current, pageSize), query);
        List<AttachmentVO> records = page.getRecords()
                .stream()
                .map(attachment -> AttachmentVO.of(
                        attachment,
                        talentOposDataSourceService.getSupplierById(attachment.getSupplierGuid())))
                .toList();
        return PageResponseVO.of(page, records);
    }

    @Override
    public AttachmentVO upload(MultipartFile file, String supplierGuid, AttachmentOwner ownerType) {
        requireSupplierGuid(supplierGuid);
        validateFile(file);
        // 校验供应商是否存在
        Supplier supplier = talentOposDataSourceService.getSupplierById(supplierGuid);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        ImportAttachment attachment = buildAttachment(file, supplierGuid, ownerType);
        saveFile(file, attachment);
        importAttachmentMapper.insert(attachment);
        return AttachmentVO.of(attachment, supplier);
    }

    @Override
    public BatchUploadResultVO batchUpload(List<MultipartFile> files, List<String> supplierGuids,
                                           AttachmentOwner ownerType) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请至少上传一个附件");
        }
        if (supplierGuids == null || supplierGuids.size() != files.size()) {
            throw new BusinessException("每个附件都必须指定供应商");
        }

        List<BatchUploadItemVO> items = new ArrayList<>();
        int successCount = 0;
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String supplierGuid = supplierGuids.get(i);
            String fileName = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
            try {
                AttachmentVO vo = upload(file, supplierGuid, ownerType);
                successCount++;
                items.add(BatchUploadItemVO.success(fileName, vo));
            } catch (BusinessException ex) {
                items.add(BatchUploadItemVO.fail(fileName, ex.getMessage()));
            }
        }

        return BatchUploadResultVO.builder()
                .total(files.size())
                .successCount(successCount)
                .failureCount(files.size() - successCount)
                .items(items)
                .build();
    }

    @Override
    public List<BatchUploadSupplierMatchVO> matchSuppliersByFileName(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new BusinessException("fileNames 不能为空");
        }
        List<Supplier> suppliers = listSuppliersQuietly();
        return fileNames.stream()
                .map(fileName -> supplierFileNameMatcher.match(fileName, suppliers))
                .toList();
    }

    @Override
    public DownloadFileVO getDownloadFile(String uuid) {
        requireUuid(uuid);
        ImportAttachment attachment = importAttachmentMapper.selectById(uuid);
        if (attachment == null) {
            throw new BusinessException("附件不存在: " + uuid);
        }
        Path path = resolveAttachmentPath(attachment);
        if (!Files.exists(path)) {
            throw new BusinessException("附件文件不存在: " + attachment.getFileName());
        }
        return DownloadFileVO.builder()
                .path(path)
                .filename(attachment.getFileName() + "." + attachment.getExtensionName())
                .build();
    }

    @Override
    public void delete(String uuid) {
        requireUuid(uuid);
        ImportAttachment attachment = importAttachmentMapper.selectById(uuid);
        if (attachment == null) {
            throw new BusinessException("附件不存在: " + uuid);
        }
        if (ImportStatus.SUCCESS.equals(attachment.getImportStatus())) {
            throw new BusinessException("导入成功的电子发票不可删除");
        }
        try {
            invoiceArchiveService.deleteByFileId(uuid);
            Files.deleteIfExists(resolveAttachmentPath(attachment));
            importAttachmentMapper.deleteById(attachment.getUuid());
        } catch (IOException ex) {
            throw new BusinessException("删除附件文件失败: " + ex.getMessage(), ex);
        }
    }

    private void requireUuid(String uuid) {
        if (StrUtil.isBlank(uuid)) {
            throw new BusinessException("uuid 不能为空");
        }
    }

    private void requireSupplierGuid(String supplierGuid) {
        if (StrUtil.isBlank(supplierGuid)) {
            throw new BusinessException("supplierGuid 不能为空");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("附件不能为空");
        }
        String extension = extensionName(file.getOriginalFilename());
        if (!AttachmentExtension.isAllowed(extension)) {
            throw new BusinessException("附件只允许上传 Excel 或 PDF 文件");
        }
    }

    @Override
    public void markCleaned(String uuid) {
        requireUuid(uuid);
        ImportAttachment attachment = importAttachmentMapper.selectById(uuid);
        if (attachment == null) {
            throw new BusinessException("附件不存在: " + uuid);
        }
        attachment.setCleanStatus(CleanStatus.CLEANED);
        importAttachmentMapper.updateById(attachment);
    }

    @Override
    public void markImported(String uuid) {
        requireUuid(uuid);
        ImportAttachment attachment = importAttachmentMapper.selectById(uuid);
        if (attachment == null) {
            throw new BusinessException("附件不存在: " + uuid);
        }
        attachment.setImportStatus(ImportStatus.SUCCESS);
        attachment.setLastImportError(null);
        importAttachmentMapper.updateById(attachment);
    }

    @Override
    public void markImportFailed(String uuid, String reason) {
        requireUuid(uuid);
        ImportAttachment attachment = importAttachmentMapper.selectById(uuid);
        if (attachment == null) {
            return;
        }
        attachment.setImportStatus(ImportStatus.FAILED);
        attachment.setLastImportError(StrUtil.sub(reason, 0, 500));
        importAttachmentMapper.updateById(attachment);
    }

    private ImportAttachment buildAttachment(MultipartFile file, String supplierGuid, AttachmentOwner ownerType) {
        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        return ImportAttachment.builder()
                .uuid(StrUtil.uuid())
                .fileName(fileNameWithoutExtension(originalFilename))
                .fileSize(file.getSize())
                .uploadDate(LocalDateTime.now())
                .extensionName(extensionName(originalFilename))
                .supplierGuid(supplierGuid)
                .importStatus(ImportStatus.NOT_IMPORTED)
                .ownerType(ownerType == null ? AttachmentOwner.SELF : ownerType)
                .cleanStatus(CleanStatus.NOT_CLEANED)
                .build();
    }

    private void saveFile(MultipartFile file, ImportAttachment attachment) {
        try {
            Path targetDir = Path.of(storeProperties.getUpload().getAttachmentDir());
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(attachment.getUuid() + "." + attachment.getExtensionName());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("附件保存失败: " + ex.getMessage(), ex);
        }
    }

    private String fileNameWithoutExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }

    private String extensionName(String filename) {
        String safeName = StrUtil.blankToDefault(filename, "");
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == safeName.length() - 1) {
            return "";
        }
        return safeName.substring(dotIndex + 1).toLowerCase();
    }

    private Path resolveAttachmentPath(ImportAttachment attachment) {
        return Path.of(storeProperties.getUpload().getAttachmentDir())
                .resolve(attachment.getUuid() + "." + attachment.getExtensionName());
    }

    private List<Supplier> listSuppliersQuietly() {
        try {
            return talentOposDataSourceService.listSuppliers();
        } catch (BusinessException ex) {
            return List.of();
        }
    }
}
