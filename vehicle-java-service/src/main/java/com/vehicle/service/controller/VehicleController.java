package com.vehicle.service.controller;

import com.vehicle.service.entity.Vehicle;
import com.vehicle.service.entity.VehicleAlert;
import com.vehicle.service.entity.VehicleTelemetry;
import com.vehicle.service.repository.VehicleAlertRepository;
import com.vehicle.service.repository.VehicleRepository;
import com.vehicle.service.repository.VehicleTelemetryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management", description = "APIs for managing vehicles and their data")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final VehicleTelemetryRepository telemetryRepository;
    private final VehicleAlertRepository alertRepository;

    @GetMapping
    @Operation(summary = "Get all vehicles", description = "Retrieve a paginated list of vehicles with optional status filtering")
    public ResponseEntity<Page<Vehicle>> getAllVehicles(
            @Parameter(description = "Vehicle status filter") @RequestParam(required = false) Vehicle.VehicleStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Vehicle> vehicles;
        
        if (status != null) {
            vehicles = vehicleRepository.findByStatus(status, pageable);
        } else {
            vehicles = vehicleRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get vehicle by ID", description = "Retrieve a specific vehicle by its ID")
    public ResponseEntity<Vehicle> getVehicle(
            @Parameter(description = "Vehicle ID") @PathVariable String vehicleId) {
        
        Optional<Vehicle> vehicle = vehicleRepository.findByVehicleId(vehicleId);
        return vehicle.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{vehicleId}/telemetry")
    @Operation(summary = "Get vehicle telemetry", description = "Retrieve telemetry data for a specific vehicle")
    public ResponseEntity<Page<VehicleTelemetry>> getVehicleTelemetry(
            @Parameter(description = "Vehicle ID") @PathVariable String vehicleId,
            @Parameter(description = "Hours to look back") @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        // Check if vehicle exists
        Optional<Vehicle> vehicle = vehicleRepository.findByVehicleId(vehicleId);
        if (vehicle.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        LocalDateTime timeThreshold = LocalDateTime.now().minusHours(hours);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<VehicleTelemetry> telemetry = telemetryRepository
            .findByVehicleIdAndTimestampAfterOrderByTimestampDesc(vehicleId, timeThreshold, pageable);
        
        return ResponseEntity.ok(telemetry);
    }

    @GetMapping("/{vehicleId}/alerts")
    @Operation(summary = "Get vehicle alerts", description = "Retrieve alerts for a specific vehicle")
    public ResponseEntity<Page<VehicleAlert>> getVehicleAlerts(
            @Parameter(description = "Vehicle ID") @PathVariable String vehicleId,
            @Parameter(description = "Filter by resolved status") @RequestParam(required = false) Boolean resolved,
            @Parameter(description = "Filter by severity") @RequestParam(required = false) VehicleAlert.AlertSeverity severity,
            @Parameter(description = "Hours to look back") @RequestParam(defaultValue = "168") int hours,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        // Check if vehicle exists
        Optional<Vehicle> vehicle = vehicleRepository.findByVehicleId(vehicleId);
        if (vehicle.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        LocalDateTime timeThreshold = LocalDateTime.now().minusHours(hours);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<VehicleAlert> alerts;
        
        if (resolved != null && severity != null) {
            alerts = alertRepository.findByVehicleIdAndResolvedAndSeverityAndTimestampAfterOrderByTimestampDesc(
                vehicleId, resolved, severity, timeThreshold, pageable);
        } else if (resolved != null) {
            alerts = alertRepository.findByVehicleIdAndResolvedAndTimestampAfterOrderByTimestampDesc(
                vehicleId, resolved, timeThreshold, pageable);
        } else if (severity != null) {
            alerts = alertRepository.findByVehicleIdAndSeverityAndTimestampAfterOrderByTimestampDesc(
                vehicleId, severity, timeThreshold, pageable);
        } else {
            alerts = alertRepository.findByVehicleIdAndTimestampAfterOrderByTimestampDesc(
                vehicleId, timeThreshold, pageable);
        }
        
        return ResponseEntity.ok(alerts);
    }
}