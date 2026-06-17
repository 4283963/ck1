package com.coldchain.vaccine.service;

import com.coldchain.vaccine.entity.Alert;
import com.coldchain.vaccine.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByAlertTimeDesc();
    }

    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findByIsResolvedFalseOrderByAlertTimeDesc();
    }

    public List<Alert> getAlertsByVehicleId(Long vehicleId) {
        return alertRepository.findByVehicleIdOrderByAlertTimeDesc(vehicleId);
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    @Transactional
    public Alert resolveAlert(Long id, String resolvedBy) {
        return alertRepository.findById(id).map(alert -> {
            alert.setIsResolved(true);
            alert.setResolvedTime(LocalDateTime.now());
            alert.setResolvedBy(resolvedBy);
            return alertRepository.save(alert);
        }).orElseThrow(() -> new RuntimeException("预警不存在，ID: " + id));
    }
}
