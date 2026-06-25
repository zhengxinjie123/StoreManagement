package com.joao.storemanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "store")
public class StoreProperties {

    private Invoice invoice = new Invoice();
    private Archive archive = new Archive();
    private GoogleDrive googleDrive = new GoogleDrive();

    @Getter
    @Setter
    public static class Invoice {
        private BigDecimal defaultTaxRate = new BigDecimal("23");
    }

    @Getter
    @Setter
    public static class Archive {
        private int maxNameSuffixAttempts = 1000;
    }

    @Getter
    @Setter
    public static class GoogleDrive {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
        private String tokenPath;
        private String folderName = "Fatura";
        private String folderId;
    }
}
