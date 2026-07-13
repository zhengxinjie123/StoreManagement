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
     * 使用当前 TALENTOPOS 动态连接执行数据库操作。
     */
    <T> T executeInSession(Function<SqlSession, T> action);

    /**
     * 使用当前 TALENTOPOS 动态连接执行事务性数据库操作。
     */
    <T> T executeInWriteTransaction(Function<SqlSession, T> action);
}
