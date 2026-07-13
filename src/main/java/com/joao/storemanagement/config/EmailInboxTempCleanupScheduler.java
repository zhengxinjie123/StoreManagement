package com.joao.storemanagement.config;

import com.joao.storemanagement.service.primary.EmailInboxTempStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailInboxTempCleanupScheduler {

    private final EmailInboxTempStore emailInboxTempStore;

    @Scheduled(cron = "0 15 * * * *")
    public void cleanExpiredAttachments() {
        int removed = emailInboxTempStore.cleanExpired();
        if (removed > 0) {
            log.info("已清理过期邮箱临时附件 {} 个", removed);
        }
    }
}
