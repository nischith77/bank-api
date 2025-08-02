package com.vehicle.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vehicle.service.entity.Vehicle;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleData {
    
    @JsonProperty("vehicle_id")
    @NotBlank
    @Size(max = 50)
    private String vehicleId;
    
    @JsonProperty("make")
    @NotBlank
    @Size(max = 50)
    private String make;
    
    @JsonProperty("model")
    @NotBlank
    @Size(max = 50)
    private String model;
    
    @JsonProperty("year")
    @NotNull
    @Min(1900)
    @Max(2030)
    private Integer year;
    
    @JsonProperty("vin")
    @Size(min = 17, max = 17)
    private String vin;
    
    @JsonProperty("license_plate")
    @Size(max = 20)
    private String licensePlate;
    
    @JsonProperty("fuel_type")
    @NotNull
    private Vehicle.FuelType fuelType;
    
    @JsonProperty("status")
    private Vehicle.VehicleStatus status = Vehicle.VehicleStatus.ACTIVE;
}