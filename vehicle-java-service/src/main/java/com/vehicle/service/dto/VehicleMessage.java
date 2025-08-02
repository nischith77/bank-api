package com.vehicle.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMessage {
    
    @JsonProperty("message_type")
    private String messageType;
    
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("data")
    private Map<String, Object> data;
}