package com.vehicle.service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_telemetry", indexes = {
    @Index(name = "idx_vehicle_telemetry_vehicle_id", columnList = "vehicle_id"),
    @Index(name = "idx_vehicle_telemetry_timestamp", columnList = "timestamp"),
    @Index(name = "idx_vehicle_telemetry_vehicle_timestamp", columnList = "vehicle_id, timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class VehicleTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String vehicleId;

    @Column(name = "timestamp", nullable = false)
    @NotNull
    private LocalDateTime timestamp;

    @Column(name = "latitude", precision = 10, scale = 8)
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @Column(name = "speed", precision = 5, scale = 2)
    @DecimalMin(value = "0.0")
    private BigDecimal speed; // km/h

    @Column(name = "heading", precision = 5, scale = 2)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "359.99")
    private BigDecimal heading; // degrees

    @Column(name = "altitude", precision = 8, scale = 2)
    private BigDecimal altitude; // meters

    @Column(name = "fuel_level", precision = 5, scale = 2)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal fuelLevel; // percentage

    @Column(name = "engine_temperature", precision = 5, scale = 2)
    private BigDecimal engineTemperature; // celsius

    @Column(name = "odometer", precision = 10, scale = 2)
    @DecimalMin(value = "0.0")
    private BigDecimal odometer; // kilometers

    @Column(name = "battery_voltage", precision = 4, scale = 2)
    @DecimalMin(value = "0.0")
    private BigDecimal batteryVoltage; // volts

    @Column(name = "engine_status")
    private Boolean engineStatus;

    @Column(name = "ac_status")
    private Boolean acStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", referencedColumnName = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicle;
}