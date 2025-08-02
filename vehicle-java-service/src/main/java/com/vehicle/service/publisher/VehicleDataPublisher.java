package com.vehicle.service.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.vehicle.service.dto.*;
import com.vehicle.service.entity.Vehicle;
import com.vehicle.service.entity.VehicleAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleDataPublisher {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vehicle.pubsub.topics.telemetry}")
    private String telemetryTopic;

    @Value("${vehicle.pubsub.topics.alerts}")
    private String alertsTopic;

    @Value("${vehicle.pubsub.topics.updates}")
    private String updatesTopic;

    private final List<String> vehicleIds = Arrays.asList("VH001", "VH002", "VH003", "VH004", "VH005");
    private final Random random = new Random();

    // Sample locations (lat, lon) for different cities
    private final List<Location> locations = Arrays.asList(
        new Location(new BigDecimal("40.7128"), new BigDecimal("-74.0060")), // New York
        new Location(new BigDecimal("34.0522"), new BigDecimal("-118.2437")), // Los Angeles
        new Location(new BigDecimal("41.8781"), new BigDecimal("-87.6298")), // Chicago
        new Location(new BigDecimal("29.7604"), new BigDecimal("-95.3698")), // Houston
        new Location(new BigDecimal("33.4484"), new BigDecimal("-112.0740")) // Phoenix
    );

    private record Location(BigDecimal latitude, BigDecimal longitude) {}

    // Scheduled task to publish telemetry data every 30 seconds
    @Scheduled(fixedRate = 30000)
    public void publishScheduledTelemetry() {
        try {
            publishTelemetryBatch(random.nextInt(5) + 5); // 5-10 messages
        } catch (Exception e) {
            log.error("Error in scheduled telemetry publishing: {}", e.getMessage(), e);
        }
    }

    // Scheduled task to publish alerts occasionally
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void publishScheduledAlerts() {
        try {
            if (random.nextDouble() < 0.3) { // 30% chance
                publishAlertBatch(random.nextInt(3) + 1); // 1-3 alerts
            }
        } catch (Exception e) {
            log.error("Error in scheduled alert publishing: {}", e.getMessage(), e);
        }
    }

    // Scheduled task to publish vehicle updates rarely
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void publishScheduledVehicleUpdates() {
        try {
            if (random.nextDouble() < 0.1) { // 10% chance
                publishVehicleUpdate();
            }
        } catch (Exception e) {
            log.error("Error in scheduled vehicle update publishing: {}", e.getMessage(), e);
        }
    }

    public void publishTelemetryBatch(int count) {
        log.info("Publishing {} telemetry messages", count);
        
        for (int i = 0; i < count; i++) {
            String vehicleId = vehicleIds.get(random.nextInt(vehicleIds.size()));
            TelemetryData telemetryData = generateTelemetryData(vehicleId);
            
            VehicleMessage message = new VehicleMessage(
                "telemetry",
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                objectMapper.convertValue(telemetryData, Map.class)
            );
            
            publishMessage(telemetryTopic, message);
        }
    }

    public void publishAlertBatch(int count) {
        log.info("Publishing {} alert messages", count);
        
        for (int i = 0; i < count; i++) {
            String vehicleId = vehicleIds.get(random.nextInt(vehicleIds.size()));
            AlertData alertData = generateAlertData(vehicleId);
            
            VehicleMessage message = new VehicleMessage(
                "alert",
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                objectMapper.convertValue(alertData, Map.class)
            );
            
            publishMessage(alertsTopic, message);
        }
    }

    public void publishVehicleUpdate() {
        log.info("Publishing vehicle update");
        
        VehicleData vehicleData = generateVehicleData();
        
        VehicleMessage message = new VehicleMessage(
            "vehicle_update",
            UUID.randomUUID().toString(),
            LocalDateTime.now(),
            objectMapper.convertValue(vehicleData, Map.class)
        );
        
        publishMessage(updatesTopic, message);
    }

    private void publishMessage(String topic, VehicleMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            pubSubTemplate.publish(topic, messageJson);
            log.debug("Published message to {}: {}", topic, message.getMessageId());
        } catch (Exception e) {
            log.error("Failed to publish message to {}: {}", topic, e.getMessage(), e);
        }
    }

    private TelemetryData generateTelemetryData(String vehicleId) {
        Location baseLocation = locations.get(random.nextInt(locations.size()));
        
        // Add small random variations to simulate movement
        BigDecimal latOffset = BigDecimal.valueOf(random.nextDouble() * 0.02 - 0.01);
        BigDecimal lonOffset = BigDecimal.valueOf(random.nextDouble() * 0.02 - 0.01);
        
        TelemetryData data = new TelemetryData();
        data.setVehicleId(vehicleId);
        data.setTimestamp(LocalDateTime.now());
        data.setLatitude(baseLocation.latitude().add(latOffset));
        data.setLongitude(baseLocation.longitude().add(lonOffset));
        data.setSpeed(BigDecimal.valueOf(random.nextDouble() * 120)); // 0-120 km/h
        data.setHeading(BigDecimal.valueOf(random.nextDouble() * 360));
        data.setAltitude(BigDecimal.valueOf(random.nextDouble() * 1000));
        data.setFuelLevel(BigDecimal.valueOf(random.nextDouble() * 90 + 10)); // 10-100%
        data.setEngineTemperature(BigDecimal.valueOf(random.nextDouble() * 30 + 80)); // 80-110°C
        data.setOdometer(BigDecimal.valueOf(random.nextDouble() * 190000 + 10000)); // 10k-200k km
        data.setBatteryVoltage(BigDecimal.valueOf(random.nextDouble() * 2.5 + 12.0)); // 12.0-14.5V
        data.setEngineStatus(random.nextBoolean());
        data.setAcStatus(random.nextBoolean());
        
        return data;
    }

    private AlertData generateAlertData(String vehicleId) {
        String[] alertTypes = {
            "low_fuel", "engine_warning", "maintenance_due", "speed_violation",
            "battery_low", "tire_pressure", "oil_change", "brake_warning"
        };
        
        String[] messages = {
            "Low fuel level detected", "Engine temperature high", "Scheduled maintenance due",
            "Speed limit exceeded", "Battery voltage low", "Tire pressure low",
            "Oil change required", "Brake system warning"
        };
        
        int index = random.nextInt(alertTypes.length);
        VehicleAlert.AlertSeverity severity = VehicleAlert.AlertSeverity.values()[
            random.nextInt(VehicleAlert.AlertSeverity.values().length)];
        
        AlertData data = new AlertData();
        data.setVehicleId(vehicleId);
        data.setAlertType(alertTypes[index]);
        data.setSeverity(severity);
        data.setMessage(messages[index] + " for vehicle " + vehicleId);
        data.setTimestamp(LocalDateTime.now());
        data.setResolved(false);
        
        return data;
    }

    private VehicleData generateVehicleData() {
        String[][] makesModels = {
            {"Toyota", "Camry"}, {"Honda", "Civic"}, {"Ford", "F-150"},
            {"Tesla", "Model 3"}, {"BMW", "X5"}, {"Mercedes", "C-Class"},
            {"Audi", "A4"}, {"Nissan", "Altima"}, {"Hyundai", "Elantra"}
        };
        
        String[] makeModel = makesModels[random.nextInt(makesModels.length)];
        String vehicleId = "VH" + String.format("%03d", random.nextInt(900) + 100);
        
        VehicleData data = new VehicleData();
        data.setVehicleId(vehicleId);
        data.setMake(makeModel[0]);
        data.setModel(makeModel[1]);
        data.setYear(random.nextInt(7) + 2018); // 2018-2024
        data.setVin(generateRandomVin());
        data.setLicensePlate(generateRandomLicensePlate());
        data.setFuelType(Vehicle.FuelType.values()[random.nextInt(Vehicle.FuelType.values().length)]);
        data.setStatus(Vehicle.VehicleStatus.ACTIVE);
        
        return data;
    }

    private String generateRandomVin() {
        String chars = "0123456789ABCDEFGHJKLMNPRSTUVWXYZ";
        StringBuilder vin = new StringBuilder();
        for (int i = 0; i < 17; i++) {
            vin.append(chars.charAt(random.nextInt(chars.length())));
        }
        return vin.toString();
    }

    private String generateRandomLicensePlate() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder plate = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            plate.append(letters.charAt(random.nextInt(letters.length())));
        }
        plate.append("-");
        plate.append(random.nextInt(900) + 100);
        return plate.toString();
    }

    // Manual publishing methods for testing
    public void publishTestScenario() {
        log.info("Publishing test scenario data");
        
        // Publish vehicle updates first
        for (int i = 0; i < 3; i++) {
            publishVehicleUpdate();
        }
        
        // Wait a bit then publish telemetry and alerts
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        publishTelemetryBatch(20);
        publishAlertBatch(5);
        
        log.info("Test scenario completed");
    }
}