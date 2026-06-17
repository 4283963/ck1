package com.coldchain.vaccine.repository;

import com.coldchain.vaccine.entity.TransportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportRecordRepository extends JpaRepository<TransportRecord, Long> {

    List<TransportRecord> findByVehicleIdOrderByReportTimeDesc(Long vehicleId);

    @Query("SELECT t FROM TransportRecord t WHERE t.vehicleId = :vehicleId ORDER BY t.reportTime DESC LIMIT :limit")
    List<TransportRecord> findLatestByVehicleId(@Param("vehicleId") Long vehicleId, @Param("limit") int limit);

    TransportRecord findTopByVehicleIdOrderByReportTimeDesc(Long vehicleId);
}
