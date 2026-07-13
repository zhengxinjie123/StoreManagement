package com.joao.storemanagement.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单条码进价历史分析响应
 *
 * @param barcode       条码
 * @param productName   商品名称
 * @param firstPrice    首次进价
 * @param latestPrice   最新进价
 * @param changeAmount  进价涨幅（最新减首次）
 * @param detailList    采购进价明细列表
 * @param increaseNodes 历史每次涨价节点
 * @param trendPoints   进价趋势图数据点
 */
public record PriceHistoryDTO(
        String barcode,
        String productName,
        BigDecimal firstPrice,
        BigDecimal latestPrice,
        BigDecimal changeAmount,
        List<PriceHistoryDetailDTO> detailList,
        List<PriceIncreaseNodeDTO> increaseNodes,
        List<PriceTrendPointDTO> trendPoints) {

    public PriceHistoryDTO() {
        this(null, null, null, null, null, null, null, null);
    }
}
