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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TransportService {

    private static final Logger logger = LoggerFactory.getLogger(TransportService.class);
    private static final int CONSECUTIVE_THRESHOLD = 3;
    private static final String ALERT_TYPE_DATA_DISTORTION = "DATA_DISTORTION";

    private final TransportRecordRepository transportRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;

    private final ConcurrentMap<Long, ReentrantLock> vehicleLocks = new ConcurrentHashMap<>();

    public TransportService(TransportRecordRepository transportRecordRepository,
                            VehicleRepository vehicleRepository,
                            AlertRepository alertRepository) {
        this.transportRecordRepository = transportRecordRepository;
        this.vehicleRepository = vehicleRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(rollbackFor = Exception.class)
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

        checkTemperatureAbnormalityAsync(vehicle, record);

        return record;
    }

    private void checkTemperatureAbnormalityAsync(Vehicle vehicle, TransportRecord currentRecord) {
        ReentrantLock lock = vehicleLocks.computeIfAbsent(vehicle.getId(), k -> new ReentrantLock());
        boolean locked = false;
        try {
            locked = lock.tryLock();
            if (!locked) {
                logger.debug("车辆 {} 正在处理预警检测，跳过本次检查", vehicle.getPlateNumber());
                return;
            }
            checkTemperatureAbnormality(vehicle, currentRecord);
        } catch (Exception e) {
            logger.error("车辆 {} 温度异常检测失败: {}", vehicle.getPlateNumber(), e.getMessage(), e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkTemperatureAbnormality(Vehicle vehicle, TransportRecord currentRecord) {
        List<TransportRecord> recentRecords = transportRecordRepository.findLatestByVehicleId(
                vehicle.getId(), CONSECUTIVE_THRESHOLD);

        if (recentRecords.size() < CONSECUTIVE_THRESHOLD) {
            logger.debug("车辆 {} 数据不足{}条，暂不检测", vehicle.getPlateNumber(), CONSECUTIVE_THRESHOLD);
            return;
        }

        BigDecimal firstTemp = recentRecords.get(0).getTemperature();
        boolean allSame = true;
        for (TransportRecord r : recentRecords) {
            if (r.getTemperature().compareTo(firstTemp) != 0) {
                allSame = false;
                break;
            }
        }

        if (allSame) {
            logger.warn("车辆 {} 连续{}次上报温度相同: {}，触发数据失真预警检测",
                    vehicle.getPlateNumber(), CONSECUTIVE_THRESHOLD, firstTemp);

            boolean hasExistingAlert = alertRepository.existsUnresolvedByVehicleIdAndAlertType(
                    vehicle.getId(), ALERT_TYPE_DATA_DISTORTION);

            if (!hasExistingAlert) {
                Alert alert = new Alert();
                alert.setVehicleId(vehicle.getId());
                alert.setPlateNumber(vehicle.getPlateNumber());
                alert.setAlertType(ALERT_TYPE_DATA_DISTORTION);
                alert.setAlertLevel("HIGH");
                alert.setAlertMessage(String.format(
                        "车辆【%s】连续%d次上报温度均为%s°C，传感器可能卡死或上传假数据，请立即核查！",
                        vehicle.getPlateNumber(), CONSECUTIVE_THRESHOLD, firstTemp));
                alert.setTemperature(firstTemp);
                alert.setLatitude(currentRecord.getLatitude());
                alert.setLongitude(currentRecord.getLongitude());
                alert.setIsResolved(false);
                alert.setAlertTime(LocalDateTime.now());
                try {
                    alertRepository.save(alert);
                    logger.info("已为车辆 {} 创建数据失真预警", vehicle.getPlateNumber());
                } catch (DataIntegrityViolationException e) {
                    logger.debug("车辆 {} 预警已被并发创建，忽略重复插入: {}", vehicle.getPlateNumber(), e.getMessage());
                }
            } else {
                logger.debug("车辆 {} 已有未处理的数据失真预警，跳过创建", vehicle.getPlateNumber());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TransportRecord> getRecordsByVehicleId(Long vehicleId) {
        return transportRecordRepository.findByVehicleIdOrderByReportTimeDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public TransportRecord getLatestRecord(Long vehicleId) {
        return transportRecordRepository.findTopByVehicleIdOrderByReportTimeDesc(vehicleId);
    }
}
