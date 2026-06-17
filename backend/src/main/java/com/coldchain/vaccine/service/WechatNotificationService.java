package com.coldchain.vaccine.service;

import com.coldchain.vaccine.dto.WechatConfigDTO;
import com.coldchain.vaccine.entity.Alert;
import com.coldchain.vaccine.repository.AlertRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WechatNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WechatNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    private static final String SEND_MSG_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";

    private static final long TOKEN_EXPIRE_MS = 7000 * 1000;

    private final SystemConfigService configService;
    private final AlertRepository alertRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public WechatNotificationService(SystemConfigService configService, AlertRepository alertRepository) {
        this.configService = configService;
        this.alertRepository = alertRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Map<String, Object> sendAlertNotification(Long alertId, String operator) {
        Map<String, Object> result = new HashMap<>();

        if (!configService.isWechatConfigured()) {
            result.put("success", false);
            result.put("message", "微信企业号配置不完整，请先在系统配置中填写凭证");
            return result;
        }

        Alert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null) {
            result.put("success", false);
            result.put("message", "预警不存在");
            return result;
        }

        try {
            WechatConfigDTO config = configService.getWechatConfig();
            String content = buildMessageContent(alert);
            String response = sendTextMessage(config, content);

            JsonNode respNode = objectMapper.readTree(response);
            int errcode = respNode.path("errcode").asInt(-1);

            if (errcode == 0) {
                alert.setIsNotified(true);
                alert.setNotifyTime(LocalDateTime.now());
                alert.setNotifyResult("发送成功");
                alert.setNotifyBy(operator != null ? operator : "系统管理员");
                alertRepository.save(alert);
                result.put("success", true);
                result.put("message", "微信通知已发送");
                result.put("alert", alert);
                logger.info("预警 {} 微信通知发送成功，接收人: {}", alertId, buildReceiverInfo(config));
            } else {
                String errmsg = respNode.path("errmsg").asText("未知错误");
                alert.setIsNotified(false);
                alert.setNotifyResult("发送失败: " + errmsg);
                alertRepository.save(alert);
                result.put("success", false);
                result.put("message", "微信通知发送失败: " + errmsg);
                logger.error("预警 {} 微信通知发送失败: errcode={}, errmsg={}", alertId, errcode, errmsg);
            }
        } catch (Exception e) {
            logger.error("预警 {} 微信通知发送异常: {}", alertId, e.getMessage(), e);
            try {
                alert.setIsNotified(false);
                alert.setNotifyResult("发送异常: " + e.getMessage());
                alertRepository.save(alert);
            } catch (Exception ignored) {
            }
            result.put("success", false);
            result.put("message", "微信通知发送异常: " + e.getMessage());
        }

        return result;
    }

    public Map<String, Object> testConnection(WechatConfigDTO config) {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = getAccessToken(config.getCorpId(), config.getAppSecret());
            if (token != null && !token.isEmpty()) {
                result.put("success", true);
                result.put("message", "微信企业号连接测试成功");
                result.put("token", token);
            } else {
                result.put("success", false);
                result.put("message", "获取 AccessToken 失败，请检查凭证");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }
        return result;
    }

    private String buildMessageContent(Alert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨【疫苗冷链数据失真预警】\n\n");
        sb.append("车牌号: ").append(alert.getPlateNumber()).append("\n");
        sb.append("预警类型: 数据失真（传感器卡死）\n");
        sb.append("预警级别: ").append("高").append("\n");

        BigDecimal temp = alert.getTemperature();
        sb.append("异常温度: ").append(temp != null ? temp + "°C" : "未知").append("\n");

        if (alert.getLatitude() != null && alert.getLongitude() != null) {
            sb.append("当前位置: ").append(formatLocation(alert.getLatitude(), alert.getLongitude())).append("\n");
        }

        sb.append("预警时间: ").append(alert.getAlertTime() != null ? alert.getAlertTime().format(FORMATTER) : "-").append("\n");
        sb.append("预警内容: ").append(alert.getAlertMessage()).append("\n\n");
        sb.append("请立即核查车辆传感器状态，确认疫苗安全！");

        return sb.toString();
    }

    private String formatLocation(BigDecimal lat, BigDecimal lng) {
        return String.format("%.4f, %.4f", lat, lng);
    }

    private String buildReceiverInfo(WechatConfigDTO config) {
        StringBuilder sb = new StringBuilder();
        if (config.getToUser() != null && !config.getToUser().isEmpty()) {
            sb.append("User:").append(config.getToUser()).append(" ");
        }
        if (config.getToParty() != null && !config.getToParty().isEmpty()) {
            sb.append("Party:").append(config.getToParty()).append(" ");
        }
        if (config.getToTag() != null && !config.getToTag().isEmpty()) {
            sb.append("Tag:").append(config.getToTag()).append(" ");
        }
        return sb.toString().trim();
    }

    private String sendTextMessage(WechatConfigDTO config, String content) throws Exception {
        String accessToken = getAccessToken(config.getCorpId(), config.getAppSecret());

        Map<String, Object> msgBody = new HashMap<>();
        msgBody.put("touser", nullToEmpty(config.getToUser()));
        msgBody.put("toparty", nullToEmpty(config.getToParty()));
        msgBody.put("totag", nullToEmpty(config.getToTag()));
        msgBody.put("msgtype", "text");
        msgBody.put("agentid", Integer.parseInt(config.getAgentId()));

        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        msgBody.put("text", text);
        msgBody.put("safe", 0);

        String requestBody = objectMapper.writeValueAsString(msgBody);
        String url = String.format(SEND_MSG_URL, accessToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String getAccessToken(String corpId, String appSecret) throws Exception {
        String cacheKey = corpId + "|" + appSecret;
        CachedToken cached = tokenCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < TOKEN_EXPIRE_MS) {
            return cached.token;
        }

        String url = String.format(GET_TOKEN_URL, corpId, appSecret);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = objectMapper.readTree(response.body());

        int errcode = node.path("errcode").asInt(-1);
        if (errcode == 0) {
            String token = node.path("access_token").asText();
            tokenCache.put(cacheKey, new CachedToken(token, System.currentTimeMillis()));
            return token;
        } else {
            throw new RuntimeException("获取AccessToken失败: " + node.path("errmsg").asText());
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static class CachedToken {
        final String token;
        final long timestamp;

        CachedToken(String token, long timestamp) {
            this.token = token;
            this.timestamp = timestamp;
        }
    }
}
