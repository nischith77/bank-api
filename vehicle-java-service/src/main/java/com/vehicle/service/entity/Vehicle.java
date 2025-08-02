package com.vehicle.service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_id", unique = true, nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String vehicleId;

    @Column(name = "make", nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String make;

    @Column(name = "model", nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String model;

    @Column(name = "year", nullable = false)
    @NotNull
    @Min(1900)
    @Max(2030)
    private Integer year;

    @Column(name = "vin", unique = true, length = 17)
    @Size(min = 17, max = 17)
    private String vin;

    @Column(name = "license_plate", length = 20)
    @Size(max = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    @NotNull
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleTelemetry> telemetryRecords;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleAlert> alerts;

    public enum VehicleStatus {
        ACTIVE, INACTIVE, MAINTENANCE, RETIRED
    }

    public enum FuelType {
        GASOLINE, DIESEL, ELECTRIC, HYBRID, LPG, CNG
    }
}