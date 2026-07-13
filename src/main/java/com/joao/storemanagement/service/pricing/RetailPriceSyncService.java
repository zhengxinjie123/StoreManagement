package com.joao.storemanagement.service.pricing;

import com.joao.storemanagement.dto.pricing.RetailPriceSyncPreviewDTO;
import com.joao.storemanagement.vo.pricing.RetailPriceSyncRunLineVO;
import com.joao.storemanagement.vo.pricing.RetailPriceSyncRunVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.util.List;
import java.util.Map;

public interface RetailPriceSyncService {

    RetailPriceSyncPreviewDTO preview(long current, long pageSize, String status);

    List<String> matchedGuids();

    /**
     * 应用零售价同步并记录同步明细。
     *
     * @param productGuids 待同步商品 GUID 列表
     * @return 同步结果统计
     */
    Map<String, Object> apply(List<String> productGuids);

    /**
     * 分页查询售价同步历史。
     *
     * @param current  页码
     * @param pageSize 每页条数
     * @return 同步历史分页
     */
    PageResponseVO<RetailPriceSyncRunVO> syncHistory(long current, long pageSize);

    /**
     * 分页查询某次同步的明细。
     *
     * @param runId    同步批次 ID
     * @param current  页码
     * @param pageSize 每页条数
     * @return 同步明细分页
     */
    PageResponseVO<RetailPriceSyncRunLineVO> syncRunLines(Long runId, long current, long pageSize);

    /**
     * 预览回滚某次同步将影响哪些商品。
     *
     * @param runId 同步批次 ID
     * @return 可回滚明细列表
     */
    List<RetailPriceSyncRunLineVO> rollbackPreview(Long runId);

    /**
     * 回滚某次同步，恢复商品原售价。
     *
     * @param runId 同步批次 ID
     * @return 回滚结果统计
     */
    Map<String, Object> rollback(Long runId);
}
