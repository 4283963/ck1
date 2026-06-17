package com.coldchain.vaccine.controller;

import com.coldchain.vaccine.entity.Alert;
import com.coldchain.vaccine.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/unresolved")
    public ResponseEntity<List<Alert>> getUnresolvedAlerts() {
        return ResponseEntity.ok(alertService.getUnresolvedAlerts());
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Alert>> getAlertsByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(alertService.getAlertsByVehicleId(vehicleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        Optional<Alert> alert = alertService.getAlertById(id);
        return alert.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String resolvedBy = body.getOrDefault("resolvedBy", "系统管理员");
            Alert alert = alertService.resolveAlert(id, resolvedBy);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
