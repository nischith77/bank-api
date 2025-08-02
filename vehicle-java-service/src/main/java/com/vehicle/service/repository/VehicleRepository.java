package com.vehicle.service.repository;

import com.vehicle.service.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    
    Optional<Vehicle> findByVehicleId(String vehicleId);
    
    Page<Vehicle> findByStatus(Vehicle.VehicleStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.status = :status")
    long countByStatus(@Param("status") Vehicle.VehicleStatus status);
    
    @Query("SELECT COUNT(v) FROM Vehicle v")
    long countAllVehicles();
}