package com.joao.storemanagement.service.pricing;

import com.joao.storemanagement.dto.pricing.RetailPriceExcelPreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceMappedLinePreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceSourceRequestDTO;
import com.joao.storemanagement.entity.pricing.RetailPriceSource;
import com.joao.storemanagement.vo.pricing.RetailPriceRefLineVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface RetailPriceRefService {

    List<RetailPriceSource> listSources();

    void requireSourceExists(Long sourceId);

    RetailPriceSource createSource(RetailPriceSourceRequestDTO request);

    RetailPriceSource updateSource(Long id, RetailPriceSourceRequestDTO request);

    int clearSourceLines(Long sourceId);

    RetailPriceExcelPreviewDTO previewImportExcel(MultipartFile file);

    List<RetailPriceMappedLinePreviewDTO> previewMappedLines(
            MultipartFile file, String mappingJson, String barcodeFilter);

    Map<String, Object> importExcel(Long sourceId, MultipartFile file, String mappingJson);

    int countLines(Long sourceId);

    /**
     * 分页查询参考源明细。
     *
     * @param sourceId    参考源 ID
     * @param current     页码
     * @param pageSize    每页条数
     * @param barcode     条码过滤
     * @param productName 商品名过滤
     * @return 明细分页
     */
    PageResponseVO<RetailPriceRefLineVO> pageLines(
            Long sourceId, long current, long pageSize, String barcode, String productName);

    /**
     * 删除单条参考源明细。
     *
     * @param sourceId 参考源 ID
     * @param lineId   明细 ID
     */
    void deleteLine(Long sourceId, Long lineId);

    /**
     * 导出参考源明细 CSV。
     *
     * @param sourceId    参考源 ID
     * @param barcode     条码过滤
     * @param productName 商品名过滤
     * @return CSV 内容
     */
    String exportLinesCsv(Long sourceId, String barcode, String productName);
}
