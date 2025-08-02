package com.vehicle.service.controller;

import com.vehicle.service.entity.Vehicle;
import com.vehicle.service.entity.VehicleAlert;
import com.vehicle.service.repository.VehicleAlertRepository;
import com.vehicle.service.repository.VehicleRepository;
import com.vehicle.service.repository.VehicleTelemetryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "System", description = "System health and statistics APIs")
public class SystemController {

    private final VehicleRepository vehicleRepository;
    private final VehicleTelemetryRepository telemetryRepository;
    private final VehicleAlertRepository alertRepository;

    @GetMapping("/")
    @Operation(summary = "API Information", description = "Get basic API information")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "name", "Vehicle Data Service",
            "version", "1.0.0",
            "status", "running",
            "timestamp", LocalDateTime.now()
        ));
    }

    @GetMapping("/stats")
    @Operation(summary = "System Statistics", description = "Get comprehensive system statistics")
    public ResponseEntity<SystemStats> getStatistics() {
        // Vehicle statistics
        long totalVehicles = vehicleRepository.countAllVehicles();
        long activeVehicles = vehicleRepository.countByStatus(Vehicle.VehicleStatus.ACTIVE);
        
        // Telemetry statistics (last 24 hours)
        LocalDateTime timeThreshold24h = LocalDateTime.now().minusHours(24);
        long telemetryCount24h = telemetryRepository.countByTimestampAfter(timeThreshold24h);
        
        // Alert statistics
        long totalAlerts = alertRepository.count();
        long activeAlerts = alertRepository.countByResolved(false);
        long criticalAlerts = alertRepository.countByResolvedAndSeverity(false, VehicleAlert.AlertSeverity.CRITICAL);
        
        VehicleStats vehicleStats = new VehicleStats(totalVehicles, activeVehicles, totalVehicles - activeVehicles);
        TelemetryStats telemetryStats = new TelemetryStats(telemetryCount24h);
        AlertStats alertStats = new AlertStats(totalAlerts, activeAlerts, criticalAlerts);
        
        SystemStats stats = new SystemStats(
            vehicleStats,
            telemetryStats,
            alertStats,
            LocalDateTime.now()
        );
        
        return ResponseEntity.ok(stats);
    }

    // DTOs for statistics response
    public record VehicleStats(long total, long active, long inactive) {}
    public record TelemetryStats(long records24h) {}
    public record AlertStats(long total, long active, long critical) {}
    public record SystemStats(
        VehicleStats vehicles,
        TelemetryStats telemetry,
        AlertStats alerts,
        LocalDateTime timestamp
    ) {}
}