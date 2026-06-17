package com.coldchain.vaccine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    @Column(name = "driver_name", nullable = false)
    private String driverName;

    @Column(name = "driver_phone")
    private String driverPhone;

    @Column(name = "vaccine_type", nullable = false)
    private String vaccineType;

    @Column(name = "vaccine_batch")
    private String vaccineBatch;

    @Column(name = "vaccine_count")
    private Integer vaccineCount;

    @Column(name = "origin_province", nullable = false)
    private String originProvince;

    @Column(name = "origin_city")
    private String originCity;

    @Column(name = "dest_province", nullable = false)
    private String destProvince;

    @Column(name = "dest_city")
    private String destCity;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "expected_arrival_time")
    private LocalDateTime expectedArrivalTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "在途";
        }
    }
}
