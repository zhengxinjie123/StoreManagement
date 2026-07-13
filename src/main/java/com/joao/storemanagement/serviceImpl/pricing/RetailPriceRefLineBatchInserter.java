package com.joao.storemanagement.serviceImpl.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceRefLine;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 浣跨敤 JDBC batchUpdate 鎵归噺鍏ュ簱銆傞伩鍏嶏細
 * 1. 澶氬€?INSERT 鐨?BigDecimal 缁戝弬閿欎綅
 * 2. MyBatis ExecutorType.BATCH 涓?@Transactional 鍐茬獊锛圕annot change the ExecutorType锛?
 */
@Component
public class RetailPriceRefLineBatchInserter {

    static final int FLUSH_SIZE = 200;

    private static final String INSERT_SQL =
            """
            INSERT INTO retail_price_ref_lines (source_id, barcode, product_name, purchase_price_tax, retail_price)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public RetailPriceRefLineBatchInserter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void insertAll(List<RetailPriceRefLine> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_SQL, rows, FLUSH_SIZE, this::bindLine);
    }

    private void bindLine(PreparedStatement ps, RetailPriceRefLine row) throws SQLException {
        ps.setLong(1, row.getSourceId());
        ps.setString(2, row.getBarcode());
        ps.setString(3, row.getProductName());
        ps.setBigDecimal(4, row.getPurchasePriceTax());
        ps.setBigDecimal(5, row.getRetailPrice());
    }
}


