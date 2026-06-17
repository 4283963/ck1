package com.coldchain.vaccine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000;
    private static volatile long lastWindowReset = System.currentTimeMillis();

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("参数错误: {}", e.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler({
            ConcurrentModificationException.class,
            ConcurrencyFailureException.class,
            CannotAcquireLockException.class
    })
    public ResponseEntity<Map<String, Object>> handleConcurrency(Exception e) {
        logRateLimited("concurrency", "并发冲突: " + e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "系统繁忙，请稍后重试");
        result.put("retryable", true);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<Map<String, Object>> handleDbConnection(DataAccessResourceFailureException e) {
        logRateLimited("db_connection", "数据库连接异常: " + e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "数据库连接繁忙，请稍后重试");
        result.put("retryable", true);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        logRateLimited("data_integrity", "数据完整性冲突: " + e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "数据冲突，请稍后重试");
        result.put("retryable", true);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        logRateLimited("generic", "服务器内部错误: " + e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "服务器内部错误");
        result.put("retryable", true);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    private void logRateLimited(String key, String message, Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastWindowReset > WINDOW_MS) {
            errorCounts.clear();
            lastWindowReset = now;
        }
        AtomicInteger count = errorCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();
        if (current <= 5) {
            logger.error(message, e);
        } else if (current == 6) {
            logger.error("同类错误[{}]触发频率限制，后续同类错误将降级为warn级别", key);
        } else {
            logger.warn("同类错误[{}]已发生{}次: {}", key, current, message);
        }
    }
}
