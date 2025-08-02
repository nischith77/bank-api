package com.vehicle.service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_alerts", indexes = {
    @Index(name = "idx_vehicle_alerts_vehicle_id", columnList = "vehicle_id"),
    @Index(name = "idx_vehicle_alerts_timestamp", columnList = "timestamp"),
    @Index(name = "idx_vehicle_alerts_resolved", columnList = "resolved")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class VehicleAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String vehicleId;

    @Column(name = "alert_type", nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    @NotNull
    private AlertSeverity severity;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String message;

    @Column(name = "timestamp", nullable = false)
    @NotNull
    private LocalDateTime timestamp;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", referencedColumnName = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicle;

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}