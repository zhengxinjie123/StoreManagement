package com.joao.storemanagement.service.talent;

import com.joao.storemanagement.dto.primary.BatchRetryImportDTO;
import com.joao.storemanagement.vo.primary.BatchRetryImportResultVO;
import com.joao.storemanagement.vo.talent.TalentPurchaseImportWorkflowVO;

public interface TalentPurchaseImportWorkflowService {

    /**
     * 导入电子发票到 TALENTOPOS。
     *
     * @param attachmentUuid 附件 UUID
     * @param templateId     清洗模板 ID，未归档时必填
     * @return 导入结果
     */
    TalentPurchaseImportWorkflowVO importAttachment(String attachmentUuid, Long templateId);

    /**
     * 重试导入失败的电子发票。
     *
     * @param attachmentUuid 附件 UUID
     * @param templateId     清洗模板 ID
     * @return 导入结果
     */
    TalentPurchaseImportWorkflowVO retryImport(String attachmentUuid, Long templateId);

    /**
     * 批量重试导入失败的电子发票。
     *
     * @param request 批量重试参数
     * @return 批量重试结果
     */
    BatchRetryImportResultVO batchRetryImport(BatchRetryImportDTO request);

    /**
     * 从归档记录重新导入 TALENTOPOS。
     *
     * @param archiveUuid 归档 UUID
     * @return 导入结果
     */
    TalentPurchaseImportWorkflowVO importArchive(String archiveUuid);
}
