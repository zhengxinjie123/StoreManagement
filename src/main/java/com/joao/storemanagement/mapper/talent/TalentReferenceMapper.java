package com.joao.storemanagement.mapper.talent;

import com.joao.storemanagement.vo.talent.TalentReferenceOptionVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TalentReferenceMapper {

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), NULLIF(NameP, ''), [No]) AS label
            FROM dbo.ProductTypes
            ORDER BY [No]
            """)
    List<TalentReferenceOptionVO> listProductTypes();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), [No]) AS label
            FROM dbo.ProductUnits
            ORDER BY [No]
            """)
    List<TalentReferenceOptionVO> listProductUnits();

    @Select("""
            SELECT COALESCE(NULLIF(Name, ''), [No]) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), [No]) AS label
            FROM dbo.ProductUnits
            ORDER BY [No]
            """)
    List<TalentReferenceOptionVO> listProductUnitNames();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), [No]) AS label
            FROM dbo.Depots
            ORDER BY [No]
            """)
    List<TalentReferenceOptionVO> listDepots();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), [No]) AS label
            FROM dbo.LabelStyles
            WHERE Type = 0
            ORDER BY [No], Name
            """)
    List<TalentReferenceOptionVO> listLabelStyles();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), [No]) AS label
            FROM dbo.LabelStyles
            WHERE Type = 1
            ORDER BY [No], Name
            """)
    List<TalentReferenceOptionVO> listProductLabelStyles();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), NULLIF(NameP, ''), [No]) AS label
            FROM dbo.Employees
            ORDER BY [No]
            """)
    List<TalentReferenceOptionVO> listEmployees();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), [No]) AS label
            FROM dbo.Users
            ORDER BY [No], Name
            """)
    List<TalentReferenceOptionVO> listUsers();

    @Select("""
            SELECT CAST(GUID AS varchar(36)) AS value,
                   [No] AS code,
                   COALESCE(NULLIF(Name, ''), NULLIF(NameP, ''), [No]) AS label
            FROM dbo.SupplierTypes
            ORDER BY [No]
            """)
    List<TalentReferenceOptionVO> listSupplierTypes();
}
