package com.joao.storemanagement.service.talent;

import com.joao.storemanagement.dto.talent.DataSourceDTO;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.vo.talent.DataSourceStatusVO;

import java.util.List;

/**
 * TALENTOPOS 动态数据源服务。
 */
public interface TalentOposDataSourceService {

    /**
     * 保存并应用数据源配置。
     */
    DataSourceStatusVO save(DataSourceDTO form);

    /**
     * 查询当前数据源状态。
     */
    DataSourceStatusVO status();

    /**
     * 统计商品数量，用于连接验证。
     */
    long countProducts();

    /**
     * 查询供应商列表。
     */
    List<Supplier> listSuppliers();

    /**
     * 按 GUID 查询供应商。
     */
    Supplier getSupplierById(String supplierGuid);

    /**
     * 校验供应商存在并返回规范化 GUID。
     */
    String validateSupplier(String supplierGuid);
}
