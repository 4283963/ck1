package com.coldchain.vaccine.controller;

import com.coldchain.vaccine.dto.WechatConfigDTO;
import com.coldchain.vaccine.service.SystemConfigService;
import com.coldchain.vaccine.service.WechatNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class SystemConfigController {

    private final SystemConfigService configService;
    private final WechatNotificationService wechatService;

    public SystemConfigController(SystemConfigService configService, WechatNotificationService wechatService) {
        this.configService = configService;
        this.wechatService = wechatService;
    }

    @GetMapping("/wechat")
    public ResponseEntity<Map<String, Object>> getWechatConfig() {
        WechatConfigDTO config = configService.getWechatConfig();
        boolean configured = configService.isWechatConfigured();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", config);
        result.put("configured", configured);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/wechat")
    public ResponseEntity<Map<String, Object>> saveWechatConfig(@RequestBody WechatConfigDTO dto) {
        configService.saveWechatConfig(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "微信配置已保存");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/wechat/test")
    public ResponseEntity<Map<String, Object>> testWechatConnection(@RequestBody WechatConfigDTO dto) {
        Map<String, Object> result = wechatService.testConnection(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", configService.getAllConfigs());
        return ResponseEntity.ok(result);
    }
}
