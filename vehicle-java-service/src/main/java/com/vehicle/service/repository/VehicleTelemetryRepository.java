package com.vehicle.service.repository;

import com.vehicle.service.entity.VehicleTelemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleTelemetryRepository extends JpaRepository<VehicleTelemetry, UUID> {
    
    Page<VehicleTelemetry> findByVehicleIdAndTimestampAfterOrderByTimestampDesc(
        String vehicleId, LocalDateTime timestamp, Pageable pageable);
    
    @Query("SELECT t FROM VehicleTelemetry t WHERE t.timestamp >= :timestamp ORDER BY t.timestamp DESC")
    Page<VehicleTelemetry> findByTimestampAfterOrderByTimestampDesc(
        @Param("timestamp") LocalDateTime timestamp, Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM VehicleTelemetry t WHERE t.timestamp >= :timestamp")
    long countByTimestampAfter(@Param("timestamp") LocalDateTime timestamp);
    
    @Query("""
        SELECT t FROM VehicleTelemetry t 
        WHERE t.id IN (
            SELECT MAX(t2.id) FROM VehicleTelemetry t2 
            GROUP BY t2.vehicleId
        ) 
        ORDER BY t.timestamp DESC
        """)
    List<VehicleTelemetry> findLatestTelemetryForEachVehicle(Pageable pageable);
}