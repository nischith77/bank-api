package com.vehicle.service.repository;

import com.vehicle.service.entity.VehicleAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface VehicleAlertRepository extends JpaRepository<VehicleAlert, UUID> {
    
    Page<VehicleAlert> findByVehicleIdAndTimestampAfterOrderByTimestampDesc(
        String vehicleId, LocalDateTime timestamp, Pageable pageable);
    
    Page<VehicleAlert> findByVehicleIdAndResolvedAndTimestampAfterOrderByTimestampDesc(
        String vehicleId, Boolean resolved, LocalDateTime timestamp, Pageable pageable);
    
    Page<VehicleAlert> findByVehicleIdAndSeverityAndTimestampAfterOrderByTimestampDesc(
        String vehicleId, VehicleAlert.AlertSeverity severity, LocalDateTime timestamp, Pageable pageable);
    
    Page<VehicleAlert> findByVehicleIdAndResolvedAndSeverityAndTimestampAfterOrderByTimestampDesc(
        String vehicleId, Boolean resolved, VehicleAlert.AlertSeverity severity, LocalDateTime timestamp, Pageable pageable);
    
    Page<VehicleAlert> findByResolvedOrderByTimestampDesc(Boolean resolved, Pageable pageable);
    
    Page<VehicleAlert> findByResolvedAndSeverityOrderByTimestampDesc(
        Boolean resolved, VehicleAlert.AlertSeverity severity, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM VehicleAlert a WHERE a.resolved = :resolved")
    long countByResolved(@Param("resolved") Boolean resolved);
    
    @Query("SELECT COUNT(a) FROM VehicleAlert a WHERE a.resolved = :resolved AND a.severity = :severity")
    long countByResolvedAndSeverity(@Param("resolved") Boolean resolved, @Param("severity") VehicleAlert.AlertSeverity severity);
}