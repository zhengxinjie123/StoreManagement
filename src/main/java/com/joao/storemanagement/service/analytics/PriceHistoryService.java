package com.joao.storemanagement.service.analytics;

import com.joao.storemanagement.dto.analytics.PriceHistoryDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseDTO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PriceHistoryService {

    /**
     * 根据条码查询单商品进价历史分析。
     *
     * @param rawBarcode 条码
     * @return 进价历史分析结果（含趋势图与涨价节点）
     */
    PriceHistoryDTO history(String rawBarcode);

    /**
     * 分页查询进价涨幅预警。
     *
     * @param minChangeAmount  最低涨幅金额
     * @param barcode          条码过滤
     * @param supplierName     供应商名称过滤
     * @param fromPurchaseDate 最近采购日期开始
     * @param toPurchaseDate   最近采购日期结束
     * @param current          页码
     * @param pageSize         每页条数
     * @return 分页结果
     */
    PageResponseVO<PriceIncreaseDTO> priceAlerts(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate,
            long current,
            long pageSize);

    /**
     * 导出进价涨幅预警 CSV。
     *
     * @param minChangeAmount  最低涨幅金额
     * @param barcode          条码过滤
     * @param supplierName     供应商名称过滤
     * @param fromPurchaseDate 最近采购日期开始
     * @param toPurchaseDate   最近采购日期结束
     * @return CSV 内容
     */
    String exportPriceAlertsCsv(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate);
}
