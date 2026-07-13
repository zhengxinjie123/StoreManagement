package com.joao.storemanagement.service.security;

import com.joao.storemanagement.entity.security.OperationLog;
import com.joao.storemanagement.mapper.primary.OperationLogMapper;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperationLogWriter {

    private static final Logger log = LoggerFactory.getLogger(OperationLogWriter.class);

    private final OperationLogMapper operationLogMapper;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "operation-log-writer");
                thread.setDaemon(true);
                return thread;
            });

    public OperationLogWriter(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void submit(OperationLog operationLog) {
        executor.execute(() -> {
            try {
                operationLogMapper.insert(operationLog);
            } catch (Exception ex) {
                log.warn("Failed to persist operation log for {} {}", operationLog.getHttpMethod(), operationLog.getRequestUri(), ex);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
