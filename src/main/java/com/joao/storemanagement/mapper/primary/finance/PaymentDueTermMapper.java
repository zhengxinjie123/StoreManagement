package com.joao.storemanagement.mapper.primary.finance;

import com.joao.storemanagement.entity.finance.PaymentDueTerm;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentDueTermMapper {

    List<PaymentDueTerm> selectActiveOrdered();

    List<PaymentDueTerm> selectAllOrdered();

    PaymentDueTerm selectByCode(String code);

    PaymentDueTerm selectById(Long id);

    int update(PaymentDueTerm row);
}


