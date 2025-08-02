package com.vehicle.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.vehicle.service.dto.*;
import com.vehicle.service.entity.Vehicle;
import com.vehicle.service.entity.VehicleAlert;
import com.vehicle.service.entity.VehicleTelemetry;
import com.vehicle.service.repository.VehicleAlertRepository;
import com.vehicle.service.repository.VehicleRepository;
import com.vehicle.service.repository.VehicleTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;

@Service
@Configuration
@RequiredArgsConstructor
@Slf4j
public class VehicleDataConsumerService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTelemetryRepository telemetryRepository;
    private final VehicleAlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final PubSubTemplate pubSubTemplate;

    @Value("${vehicle.pubsub.subscriptions.telemetry}")
    private String telemetrySubscription;

    @Value("${vehicle.pubsub.subscriptions.alerts}")
    private String alertsSubscription;

    @Value("${vehicle.pubsub.subscriptions.updates}")
    private String updatesSubscription;

    // Channel for telemetry messages
    @Bean
    public MessageChannel telemetryInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter telemetryChannelAdapter() {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
            pubSubTemplate, telemetrySubscription);
        adapter.setOutputChannel(telemetryInputChannel());
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    // Channel for alerts messages
    @Bean
    public MessageChannel alertsInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter alertsChannelAdapter() {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
            pubSubTemplate, alertsSubscription);
        adapter.setOutputChannel(alertsInputChannel());
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    // Channel for vehicle updates messages
    @Bean
    public MessageChannel updatesInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter updatesChannelAdapter() {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
            pubSubTemplate, updatesSubscription);
        adapter.setOutputChannel(updatesInputChannel());
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @ServiceActivator(inputChannel = "telemetryInputChannel")
    public void processTelemetryMessage(
            @Payload String payload,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        
        try {
            log.debug("Received telemetry message: {}", payload);
            
            VehicleMessage vehicleMessage = objectMapper.readValue(payload, VehicleMessage.class);
            TelemetryData telemetryData = objectMapper.convertValue(vehicleMessage.getData(), TelemetryData.class);
            
            if (processTelemetryData(telemetryData)) {
                message.ack();
                log.debug("Successfully processed telemetry for vehicle: {}", telemetryData.getVehicleId());
            } else {
                message.nack();
                log.warn("Failed to process telemetry for vehicle: {}", telemetryData.getVehicleId());
            }
            
        } catch (Exception e) {
            log.error("Error processing telemetry message: {}", e.getMessage(), e);
            message.nack();
        }
    }

    @ServiceActivator(inputChannel = "alertsInputChannel")
    public void processAlertMessage(
            @Payload String payload,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        
        try {
            log.debug("Received alert message: {}", payload);
            
            VehicleMessage vehicleMessage = objectMapper.readValue(payload, VehicleMessage.class);
            AlertData alertData = objectMapper.convertValue(vehicleMessage.getData(), AlertData.class);
            
            if (processAlertData(alertData)) {
                message.ack();
                log.info("Successfully processed {} alert for vehicle: {}", 
                    alertData.getSeverity(), alertData.getVehicleId());
            } else {
                message.nack();
                log.warn("Failed to process alert for vehicle: {}", alertData.getVehicleId());
            }
            
        } catch (Exception e) {
            log.error("Error processing alert message: {}", e.getMessage(), e);
            message.nack();
        }
    }

    @ServiceActivator(inputChannel = "updatesInputChannel")
    public void processUpdateMessage(
            @Payload String payload,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        
        try {
            log.debug("Received vehicle update message: {}", payload);
            
            VehicleMessage vehicleMessage = objectMapper.readValue(payload, VehicleMessage.class);
            VehicleData vehicleData = objectMapper.convertValue(vehicleMessage.getData(), VehicleData.class);
            
            if (processVehicleData(vehicleData)) {
                message.ack();
                log.info("Successfully processed vehicle update for: {}", vehicleData.getVehicleId());
            } else {
                message.nack();
                log.warn("Failed to process vehicle update for: {}", vehicleData.getVehicleId());
            }
            
        } catch (Exception e) {
            log.error("Error processing vehicle update message: {}", e.getMessage(), e);
            message.nack();
        }
    }

    @Transactional
    public boolean processTelemetryData(TelemetryData data) {
        try {
            // Validate data
            Set<ConstraintViolation<TelemetryData>> violations = validator.validate(data);
            if (!violations.isEmpty()) {
                log.warn("Invalid telemetry data: {}", violations);
                return false;
            }

            // Check if vehicle exists
            Optional<Vehicle> vehicleOpt = vehicleRepository.findByVehicleId(data.getVehicleId());
            if (vehicleOpt.isEmpty()) {
                log.warn("Vehicle {} not found, skipping telemetry", data.getVehicleId());
                return false;
            }

            // Create telemetry record
            VehicleTelemetry telemetry = new VehicleTelemetry();
            telemetry.setVehicleId(data.getVehicleId());
            telemetry.setTimestamp(data.getTimestamp());
            telemetry.setLatitude(data.getLatitude());
            telemetry.setLongitude(data.getLongitude());
            telemetry.setSpeed(data.getSpeed());
            telemetry.setHeading(data.getHeading());
            telemetry.setAltitude(data.getAltitude());
            telemetry.setFuelLevel(data.getFuelLevel());
            telemetry.setEngineTemperature(data.getEngineTemperature());
            telemetry.setOdometer(data.getOdometer());
            telemetry.setBatteryVoltage(data.getBatteryVoltage());
            telemetry.setEngineStatus(data.getEngineStatus());
            telemetry.setAcStatus(data.getAcStatus());

            telemetryRepository.save(telemetry);
            return true;

        } catch (Exception e) {
            log.error("Error processing telemetry data: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean processAlertData(AlertData data) {
        try {
            // Validate data
            Set<ConstraintViolation<AlertData>> violations = validator.validate(data);
            if (!violations.isEmpty()) {
                log.warn("Invalid alert data: {}", violations);
                return false;
            }

            // Check if vehicle exists
            Optional<Vehicle> vehicleOpt = vehicleRepository.findByVehicleId(data.getVehicleId());
            if (vehicleOpt.isEmpty()) {
                log.warn("Vehicle {} not found, skipping alert", data.getVehicleId());
                return false;
            }

            // Create alert record
            VehicleAlert alert = new VehicleAlert();
            alert.setVehicleId(data.getVehicleId());
            alert.setAlertType(data.getAlertType());
            alert.setSeverity(data.getSeverity());
            alert.setMessage(data.getMessage());
            alert.setTimestamp(data.getTimestamp());
            alert.setResolved(data.getResolved());
            alert.setResolvedAt(data.getResolvedAt());

            alertRepository.save(alert);
            return true;

        } catch (Exception e) {
            log.error("Error processing alert data: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean processVehicleData(VehicleData data) {
        try {
            // Validate data
            Set<ConstraintViolation<VehicleData>> violations = validator.validate(data);
            if (!violations.isEmpty()) {
                log.warn("Invalid vehicle data: {}", violations);
                return false;
            }

            // Check if vehicle exists
            Optional<Vehicle> existingVehicle = vehicleRepository.findByVehicleId(data.getVehicleId());
            
            Vehicle vehicle;
            if (existingVehicle.isPresent()) {
                // Update existing vehicle
                vehicle = existingVehicle.get();
                vehicle.setMake(data.getMake());
                vehicle.setModel(data.getModel());
                vehicle.setYear(data.getYear());
                vehicle.setVin(data.getVin());
                vehicle.setLicensePlate(data.getLicensePlate());
                vehicle.setFuelType(data.getFuelType());
                vehicle.setStatus(data.getStatus());
                log.info("Updated existing vehicle: {}", data.getVehicleId());
            } else {
                // Create new vehicle
                vehicle = new Vehicle();
                vehicle.setVehicleId(data.getVehicleId());
                vehicle.setMake(data.getMake());
                vehicle.setModel(data.getModel());
                vehicle.setYear(data.getYear());
                vehicle.setVin(data.getVin());
                vehicle.setLicensePlate(data.getLicensePlate());
                vehicle.setFuelType(data.getFuelType());
                vehicle.setStatus(data.getStatus());
                log.info("Created new vehicle: {}", data.getVehicleId());
            }

            vehicleRepository.save(vehicle);
            return true;

        } catch (Exception e) {
            log.error("Error processing vehicle data: {}", e.getMessage(), e);
            return false;
        }
    }
}