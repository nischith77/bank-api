package com.vehicle.service.controller;

import com.vehicle.service.entity.VehicleAlert;
import com.vehicle.service.repository.VehicleAlertRepository;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "APIs for vehicle alerts management")
public class AlertController {

    private final VehicleAlertRepository alertRepository;

    @GetMapping("/active")
    @Operation(summary = "Get active alerts", description = "Retrieve active (unresolved) alerts")
    public ResponseEntity<Page<VehicleAlert>> getActiveAlerts(
            @Parameter(description = "Filter by severity") @RequestParam(required = false) VehicleAlert.AlertSeverity severity,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 500));
        Page<VehicleAlert> alerts;
        
        if (severity != null) {
            alerts = alertRepository.findByResolvedAndSeverityOrderByTimestampDesc(false, severity, pageable);
        } else {
            alerts = alertRepository.findByResolvedOrderByTimestampDesc(false, pageable);
        }
        
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/resolved")
    @Operation(summary = "Get resolved alerts", description = "Retrieve resolved alerts")
    public ResponseEntity<Page<VehicleAlert>> getResolvedAlerts(
            @Parameter(description = "Filter by severity") @RequestParam(required = false) VehicleAlert.AlertSeverity severity,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 500));
        Page<VehicleAlert> alerts;
        
        if (severity != null) {
            alerts = alertRepository.findByResolvedAndSeverityOrderByTimestampDesc(true, severity, pageable);
        } else {
            alerts = alertRepository.findByResolvedOrderByTimestampDesc(true, pageable);
        }
        
        return ResponseEntity.ok(alerts);
    }

    @PatchMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve alert", description = "Mark an alert as resolved")
    public ResponseEntity<?> resolveAlert(
            @Parameter(description = "Alert ID") @PathVariable UUID alertId) {
        
        Optional<VehicleAlert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        VehicleAlert alert = alertOpt.get();
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        
        alertRepository.save(alert);
        
        return ResponseEntity.ok().body(new AlertResolveResponse("Alert resolved successfully", alertId));
    }

    // Response DTO for alert resolution
    public record AlertResolveResponse(String message, UUID alertId) {}
}