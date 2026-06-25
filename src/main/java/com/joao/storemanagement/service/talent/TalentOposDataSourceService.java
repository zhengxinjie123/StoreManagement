package com.joao.storemanagement.service.talent;

import com.joao.storemanagement.dto.talent.DataSourceDTO;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.vo.talent.DataSourceStatusVO;
import com.joao.storemanagement.vo.talent.TalentReferenceOptionVO;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * TALENTOPOS 动态数据源服务。
 */
public interface TalentOposDataSourceService {

    DataSourceStatusVO save(DataSourceDTO form);

    DataSourceStatusVO status();

    long countProducts();

    List<Supplier> listSuppliers();

    List<TalentReferenceOptionVO> listReferenceOptions(String type);

    Map<String, List<TalentReferenceOptionVO>> listReferenceOptionMap();

    Supplier getSupplierById(String supplierGuid);

    String validateSupplier(String supplierGuid);

    /**
     * 在 TALENTOPOS 写事务中执行操作（采购入库等）。
     */
    <T> T executeInWriteTransaction(Function<SqlSession, T> action);
}
