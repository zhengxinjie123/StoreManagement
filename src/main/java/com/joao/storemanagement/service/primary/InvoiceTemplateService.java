package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.dto.primary.InvoiceTemplateDTO;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.vo.primary.InvoiceTemplateVO;
import com.joao.storemanagement.vo.response.PageResponseVO;

import java.util.List;

/**
 * 发票清洗模板服务。
 */
public interface InvoiceTemplateService {

    /**
     * 分页查询模板。
     */
    PageResponseVO<InvoiceTemplateVO> page(
            long current, long pageSize, String supplierGuid);

    /**
     * 查询供应商可用模板列表。
     */
    List<InvoiceTemplateVO> listForSupplier(String supplierGuid);

    /**
     * 查询模板详情。
     */
    InvoiceTemplateVO get(Long id);

    /**
     * 创建模板。
     */
    InvoiceTemplateVO create(InvoiceTemplateDTO form);

    /**
     * 更新模板。
     */
    InvoiceTemplateVO update(Long id, InvoiceTemplateDTO form);

    /**
     * 删除模板。
     */
    void delete(Long id);

    /**
     * 复制模板。
     *
     * @param id 源模板 ID
     * @return 新模板
     */
    InvoiceTemplateVO copy(Long id);

    /**
     * 记录模板最近使用时间。
     *
     * @param id 模板 ID
     */
    void touchLastUsed(Long id);

    /**
     * 判断模板是否含税入库。
     */
    boolean isTaxIncluded(InvoiceTemplate template);

    /**
     * 校验模板归属并返回模板实体。
     */
    InvoiceTemplate requireTemplateForSupplier(Long templateId, String supplierGuid);
}
