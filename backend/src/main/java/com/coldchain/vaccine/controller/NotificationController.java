package com.coldchain.vaccine.controller;

import com.coldchain.vaccine.service.WechatNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final WechatNotificationService wechatService;

    public NotificationController(WechatNotificationService wechatService) {
        this.wechatService = wechatService;
    }

    @PostMapping("/wechat/alert/{alertId}")
    public ResponseEntity<Map<String, Object>> sendWechatAlert(
            @PathVariable Long alertId,
            @RequestBody(required = false) Map<String, String> body) {
        String operator = null;
        if (body != null) {
            operator = body.get("operator");
        }
        Map<String, Object> result = wechatService.sendAlertNotification(alertId, operator);
        return ResponseEntity.ok(result);
    }
}
