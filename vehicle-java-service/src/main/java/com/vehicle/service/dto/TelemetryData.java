package com.vehicle.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryData {
    
    @JsonProperty("vehicle_id")
    @NotBlank
    @Size(max = 50)
    private String vehicleId;
    
    @JsonProperty("timestamp")
    @NotNull
    private LocalDateTime timestamp;
    
    @JsonProperty("latitude")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;
    
    @JsonProperty("longitude")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;
    
    @JsonProperty("speed")
    @DecimalMin(value = "0.0")
    private BigDecimal speed;
    
    @JsonProperty("heading")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "359.99")
    private BigDecimal heading;
    
    @JsonProperty("altitude")
    private BigDecimal altitude;
    
    @JsonProperty("fuel_level")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal fuelLevel;
    
    @JsonProperty("engine_temperature")
    private BigDecimal engineTemperature;
    
    @JsonProperty("odometer")
    @DecimalMin(value = "0.0")
    private BigDecimal odometer;
    
    @JsonProperty("battery_voltage")
    @DecimalMin(value = "0.0")
    private BigDecimal batteryVoltage;
    
    @JsonProperty("engine_status")
    private Boolean engineStatus;
    
    @JsonProperty("ac_status")
    private Boolean acStatus;
}