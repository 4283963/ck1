package com.coldchain.vaccine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_alerts_is_resolved", columnList = "is_resolved")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "plate_number", nullable = false)
    private String plateNumber;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "alert_level", nullable = false)
    private String alertLevel;

    @Column(name = "alert_message", nullable = false, length = 500)
    private String alertMessage;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (alertTime == null) {
            alertTime = LocalDateTime.now();
        }
        if (isResolved == null) {
            isResolved = false;
        }
    }
}
