package com.vehicle.service.controller;

import com.vehicle.service.entity.VehicleTelemetry;
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

@RestController
@RequestMapping("/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry", description = "APIs for vehicle telemetry data")
public class TelemetryController {

    private final VehicleTelemetryRepository telemetryRepository;

    @GetMapping("/latest")
    @Operation(summary = "Get latest telemetry", description = "Retrieve the latest telemetry data across all vehicles")
    public ResponseEntity<List<VehicleTelemetry>> getLatestTelemetry(
            @Parameter(description = "Maximum number of records") @RequestParam(defaultValue = "50") int limit) {
        
        Pageable pageable = PageRequest.of(0, Math.min(limit, 500));
        List<VehicleTelemetry> telemetry = telemetryRepository.findLatestTelemetryForEachVehicle(pageable);
        
        return ResponseEntity.ok(telemetry);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent telemetry", description = "Retrieve recent telemetry data within specified time range")
    public ResponseEntity<Page<VehicleTelemetry>> getRecentTelemetry(
            @Parameter(description = "Hours to look back") @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        LocalDateTime timeThreshold = LocalDateTime.now().minusHours(hours);
        Pageable pageable = PageRequest.of(page, Math.min(size, 1000));
        
        Page<VehicleTelemetry> telemetry = telemetryRepository
            .findByTimestampAfterOrderByTimestampDesc(timeThreshold, pageable);
        
        return ResponseEntity.ok(telemetry);
    }
}