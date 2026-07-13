package com.joao.storemanagement.config;

import com.joao.storemanagement.service.security.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    private final UserSessionService userSessionService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanExpiredSessions() {
        int removed = userSessionService.cleanExpiredSessions();
        if (removed > 0) {
            log.info("已清理过期登录会话 {} 条", removed);
        }
    }
}
