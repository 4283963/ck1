package com.coldchain.vaccine.controller;

import com.coldchain.vaccine.dto.TransportDataDTO;
import com.coldchain.vaccine.entity.TransportRecord;
import com.coldchain.vaccine.service.TransportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transport")
public class TransportController {

    private final TransportService transportService;

    public TransportController(TransportService transportService) {
        this.transportService = transportService;
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> reportData(@Valid @RequestBody TransportDataDTO dto) {
        try {
            TransportRecord record = transportService.receiveTransportData(dto);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "数据上报成功");
            result.put("data", record);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "数据上报失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<TransportRecord>> getRecordsByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(transportService.getRecordsByVehicleId(vehicleId));
    }

    @GetMapping("/vehicle/{vehicleId}/latest")
    public ResponseEntity<TransportRecord> getLatestRecord(@PathVariable Long vehicleId) {
        TransportRecord record = transportService.getLatestRecord(vehicleId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }
}
