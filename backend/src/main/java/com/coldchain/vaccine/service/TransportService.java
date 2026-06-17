package com.coldchain.vaccine.service;

import com.coldchain.vaccine.dto.TransportDataDTO;
import com.coldchain.vaccine.entity.Alert;
import com.coldchain.vaccine.entity.TransportRecord;
import com.coldchain.vaccine.entity.Vehicle;
import com.coldchain.vaccine.repository.AlertRepository;
import com.coldchain.vaccine.repository.TransportRecordRepository;
import com.coldchain.vaccine.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransportService {

    private static final Logger logger = LoggerFactory.getLogger(TransportService.class);
    private static final int CONSECUTIVE_THRESHOLD = 3;

    private final TransportRecordRepository transportRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;

    public TransportService(TransportRecordRepository transportRecordRepository,
                            VehicleRepository vehicleRepository,
                            AlertRepository alertRepository) {
        this.transportRecordRepository = transportRecordRepository;
        this.vehicleRepository = vehicleRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional
    public TransportRecord receiveTransportData(TransportDataDTO dto) {
        logger.debug("收到车辆数据上报: 车牌号={}, 温度={}, 经纬度=({},{})",
                dto.getPlateNumber(), dto.getTemperature(), dto.getLatitude(), dto.getLongitude());

        Optional<Vehicle> vehicleOpt = vehicleRepository.findByPlateNumber(dto.getPlateNumber());
        if (vehicleOpt.isEmpty()) {
            throw new IllegalArgumentException("未找到车牌号为 " + dto.getPlateNumber() + " 的车辆信息");
        }

        Vehicle vehicle = vehicleOpt.get();

        TransportRecord record = new TransportRecord();
        record.setVehicleId(vehicle.getId());
        record.setPlateNumber(dto.getPlateNumber());
        record.setTemperature(dto.getTemperature());
        record.setLatitude(dto.getLatitude());
        record.setLongitude(dto.getLongitude());
        record.setLocationAddress(dto.getLocationAddress());
        record.setReportTime(LocalDateTime.now());
        record = transportRecordRepository.save(record);

        checkTemperatureAbnormality(vehicle, record);

        return record;
    }

    private void checkTemperatureAbnormality(Vehicle vehicle, TransportRecord currentRecord) {
        List<TransportRecord> recentRecords = transportRecordRepository.findLatestByVehicleId(
                vehicle.getId(), CONSECUTIVE_THRESHOLD);

        if (recentRecords.size() < CONSECUTIVE_THRESHOLD) {
            logger.debug("车辆 {} 数据不足{}条，暂不检测", vehicle.getPlateNumber(), CONSECUTIVE_THRESHOLD);
            return;
        }

        BigDecimal firstTemp = recentRecords.get(0).getTemperature();
        boolean allSame = recentRecords.stream()
                .allMatch(r -> r.getTemperature().compareTo(firstTemp) == 0);

        if (allSame) {
            logger.warn("车辆 {} 连续{}次上报温度相同: {}，触发数据失真预警",
                    vehicle.getPlateNumber(), CONSECUTIVE_THRESHOLD, firstTemp);

            List<Alert> unresolvedAlerts = alertRepository.findByIsResolvedFalseOrderByAlertTimeDesc();
            boolean hasExistingAlert = unresolvedAlerts.stream()
                    .anyMatch(a -> a.getVehicleId().equals(vehicle.getId())
                            && "DATA_DISTORTION".equals(a.getAlertType()));

            if (!hasExistingAlert) {
                Alert alert = new Alert();
                alert.setVehicleId(vehicle.getId());
                alert.setPlateNumber(vehicle.getPlateNumber());
                alert.setAlertType("DATA_DISTORTION");
                alert.setAlertLevel("HIGH");
                alert.setAlertMessage(String.format(
                        "车辆【%s】连续%d次上报温度均为%s°C，传感器可能卡死或上传假数据，请立即核查！",
                        vehicle.getPlateNumber(), CONSECUTIVE_THRESHOLD, firstTemp));
                alert.setTemperature(firstTemp);
                alert.setLatitude(currentRecord.getLatitude());
                alert.setLongitude(currentRecord.getLongitude());
                alert.setIsResolved(false);
                alert.setAlertTime(LocalDateTime.now());
                alertRepository.save(alert);
                logger.info("已为车辆 {} 创建数据失真预警", vehicle.getPlateNumber());
            } else {
                logger.debug("车辆 {} 已有未处理的数据失真预警，跳过创建", vehicle.getPlateNumber());
            }
        }
    }

    public List<TransportRecord> getRecordsByVehicleId(Long vehicleId) {
        return transportRecordRepository.findByVehicleIdOrderByReportTimeDesc(vehicleId);
    }

    public TransportRecord getLatestRecord(Long vehicleId) {
        return transportRecordRepository.findTopByVehicleIdOrderByReportTimeDesc(vehicleId);
    }
}
