package com.coldchain.vaccine.repository;

import com.coldchain.vaccine.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByIsResolvedFalseOrderByAlertTimeDesc();

    List<Alert> findByVehicleIdOrderByAlertTimeDesc(Long vehicleId);

    List<Alert> findAllByOrderByAlertTimeDesc();

    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.vehicleId = :vehicleId AND a.alertType = :alertType AND a.isResolved = false")
    boolean existsUnresolvedByVehicleIdAndAlertType(@Param("vehicleId") Long vehicleId, @Param("alertType") String alertType);
}
