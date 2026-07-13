package com.joao.storemanagement.service.pricing;

import com.joao.storemanagement.dto.pricing.RetailPriceManageUpdateDTO;
import com.joao.storemanagement.vo.pricing.RetailPriceManageItemVO;
import com.joao.storemanagement.vo.pricing.RetailPriceManualChangeVO;
import com.joao.storemanagement.vo.response.PageResponseVO;

public interface RetailPriceManageService {

    PageResponseVO<RetailPriceManageItemVO> pageMissingRetail(long current, long pageSize);

    void updateRetailPrice(String productGuid, RetailPriceManageUpdateDTO request, String username);

    PageResponseVO<RetailPriceManualChangeVO> pageChanges(long current, long pageSize);

    void rollbackChange(Long changeId, String username);
}
