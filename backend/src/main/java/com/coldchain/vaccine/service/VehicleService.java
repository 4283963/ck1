package com.coldchain.vaccine.service;

import com.coldchain.vaccine.entity.Vehicle;
import com.coldchain.vaccine.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    public Optional<Vehicle> getVehicleByPlateNumber(String plateNumber) {
        return vehicleRepository.findByPlateNumber(plateNumber);
    }

    public Vehicle createVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails) {
        return vehicleRepository.findById(id).map(vehicle -> {
            vehicle.setPlateNumber(vehicleDetails.getPlateNumber());
            vehicle.setDriverName(vehicleDetails.getDriverName());
            vehicle.setDriverPhone(vehicleDetails.getDriverPhone());
            vehicle.setVaccineType(vehicleDetails.getVaccineType());
            vehicle.setVaccineBatch(vehicleDetails.getVaccineBatch());
            vehicle.setVaccineCount(vehicleDetails.getVaccineCount());
            vehicle.setOriginProvince(vehicleDetails.getOriginProvince());
            vehicle.setOriginCity(vehicleDetails.getOriginCity());
            vehicle.setDestProvince(vehicleDetails.getDestProvince());
            vehicle.setDestCity(vehicleDetails.getDestCity());
            vehicle.setDepartureTime(vehicleDetails.getDepartureTime());
            vehicle.setExpectedArrivalTime(vehicleDetails.getExpectedArrivalTime());
            vehicle.setStatus(vehicleDetails.getStatus());
            return vehicleRepository.save(vehicle);
        }).orElseThrow(() -> new RuntimeException("车辆不存在，ID: " + id));
    }

    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}
