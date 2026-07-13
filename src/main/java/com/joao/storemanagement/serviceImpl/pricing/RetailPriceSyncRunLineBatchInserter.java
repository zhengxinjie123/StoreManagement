package com.joao.storemanagement.serviceImpl.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceSyncRunLine;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RetailPriceSyncRunLineBatchInserter {

    static final int FLUSH_SIZE = 200;

    private static final String INSERT_SQL =
            """
            INSERT INTO retail_price_sync_run_lines (
                run_id, product_guid, barcode, old_retail_price_tax, new_retail_price_tax, source_code, source_name
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public RetailPriceSyncRunLineBatchInserter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void insertAll(List<RetailPriceSyncRunLine> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_SQL, rows, FLUSH_SIZE, this::bindLine);
    }

    private void bindLine(PreparedStatement ps, RetailPriceSyncRunLine row) throws SQLException {
        ps.setLong(1, row.getRunId());
        ps.setString(2, row.getProductGuid());
        ps.setString(3, row.getBarcode());
        ps.setBigDecimal(4, row.getOldRetailPriceTax());
        ps.setBigDecimal(5, row.getNewRetailPriceTax());
        ps.setString(6, row.getSourceCode());
        ps.setString(7, row.getSourceName());
    }
}
