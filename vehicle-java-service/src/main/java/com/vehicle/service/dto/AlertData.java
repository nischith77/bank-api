package com.vehicle.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vehicle.service.entity.VehicleAlert;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertData {
    
    @JsonProperty("vehicle_id")
    @NotBlank
    @Size(max = 50)
    private String vehicleId;
    
    @JsonProperty("alert_type")
    @NotBlank
    @Size(max = 50)
    private String alertType;
    
    @JsonProperty("severity")
    @NotNull
    private VehicleAlert.AlertSeverity severity;
    
    @JsonProperty("message")
    @NotBlank
    private String message;
    
    @JsonProperty("timestamp")
    @NotNull
    private LocalDateTime timestamp;
    
    @JsonProperty("resolved")
    private Boolean resolved = false;
    
    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;
}