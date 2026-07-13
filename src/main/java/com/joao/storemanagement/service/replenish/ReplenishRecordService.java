package com.joao.storemanagement.service.replenish;

import com.joao.storemanagement.dto.replenish.PurchaseSuggestionGroupDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordRequestDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordTreeNodeDTO;
import com.joao.storemanagement.entity.replenish.ReplenishRecord;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.util.List;

public interface ReplenishRecordService {

    List<ReplenishRecordTreeNodeDTO> tree();

    List<ReplenishRecordTreeNodeDTO> completedTree();

    /**
     * 分页查询补货记录。
     *
     * @param current      页码
     * @param pageSize     每页条数
     * @param status       状态：pending / done
     * @param barcode      条码
     * @param supplierName 供应商
     * @param keyword      商品名关键字
     * @return 分页结果
     */
    PageResponseVO<ReplenishRecord> page(
            long current,
            long pageSize,
            String status,
            String barcode,
            String supplierName,
            String keyword);

    ReplenishRecord create(ReplenishRecordRequestDTO request);

    void complete(Long id);

    void reopen(Long id);

    /**
     * 更新补货备注，不影响补货状态。
     *
     * @param id     记录 ID
     * @param remark 备注
     */
    void updateRemark(Long id, String remark);

    void delete(Long id);

    /**
     * 按供应商汇总待补货采购建议。
     *
     * @return 采购建议分组
     */
    List<PurchaseSuggestionGroupDTO> purchaseSuggestions();

    /**
     * 导出采购建议 CSV。
     *
     * @return CSV 内容
     */
    String exportPurchaseSuggestionsCsv();
}
