package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailInboxTempStore {

    private static final int EXPIRE_HOURS = 4;

    private final Map<String, TempAttachment> entries = new ConcurrentHashMap<>();

    public String createSessionId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public String register(
            Long userId,
            String sessionId,
            String token,
            Path storedPath,
            String fileName,
            String extensionName,
            long fileSize) {
        TempAttachment entry = new TempAttachment(
                token,
                userId,
                sessionId,
                storedPath,
                fileName,
                extensionName,
                fileSize,
                LocalDateTime.now().plusHours(EXPIRE_HOURS));
        entries.put(entry.token(), entry);
        return entry.token();
    }

    public TempAttachment requireOwned(Long userId, String token) {
        TempAttachment entry = entries.get(token);
        if (entry == null) {
            throw new BusinessException("附件预览已过期或不存在，请重新拉取邮箱");
        }
        if (!entry.userId().equals(userId)) {
            throw new BusinessException("无权访问该附件");
        }
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            remove(token);
            throw new BusinessException("附件预览已过期，请重新拉取邮箱");
        }
        if (!Files.exists(entry.storedPath())) {
            remove(token);
            throw new BusinessException("附件文件不存在，请重新拉取邮箱");
        }
        return entry;
    }

    public void clearForUser(Long userId) {
        entries.entrySet().removeIf(item -> {
            if (!item.getValue().userId().equals(userId)) {
                return false;
            }
            deleteQuietly(item.getValue().storedPath());
            return true;
        });
    }

    public void remove(String token) {
        TempAttachment entry = entries.remove(token);
        if (entry != null) {
            deleteQuietly(entry.storedPath());
        }
    }

    public int cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        for (Map.Entry<String, TempAttachment> item : entries.entrySet()) {
            if (item.getValue().expiresAt().isBefore(now)) {
                if (entries.remove(item.getKey(), item.getValue())) {
                    deleteQuietly(item.getValue().storedPath());
                    removed++;
                }
            }
        }
        return removed;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("删除临时邮箱附件失败: {}", path, ex);
        }
    }

    public record TempAttachment(
            String token,
            Long userId,
            String sessionId,
            Path storedPath,
            String fileName,
            String extensionName,
            long fileSize,
            LocalDateTime expiresAt) {

        public String displayFileName() {
            return fileName + "." + extensionName;
        }
    }
}
