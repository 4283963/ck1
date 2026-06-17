package com.coldchain.vaccine.repository;

import com.coldchain.vaccine.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByIsResolvedFalseOrderByAlertTimeDesc();

    List<Alert> findByVehicleIdOrderByAlertTimeDesc(Long vehicleId);

    List<Alert> findAllByOrderByAlertTimeDesc();
}
