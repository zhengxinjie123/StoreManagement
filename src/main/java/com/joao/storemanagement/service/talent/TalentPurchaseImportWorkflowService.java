package com.joao.storemanagement.service.talent;

import com.joao.storemanagement.vo.talent.TalentPurchaseImportWorkflowVO;

public interface TalentPurchaseImportWorkflowService {

    TalentPurchaseImportWorkflowVO importAttachment(String attachmentUuid, Long templateId);
}
