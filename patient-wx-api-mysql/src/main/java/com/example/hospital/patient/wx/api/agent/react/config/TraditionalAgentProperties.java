package com.example.hospital.patient.wx.api.agent.react.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.react")
public class TraditionalAgentProperties {
    private boolean enabled = true;
    private long sessionTtlMinutes = 30;
    private boolean llmEnabled = true;
    private boolean confirmRequiredForWrite = true;
    private int maxSteps = 6;
    private String baseUrl;
    private String apiKey;
    private String model;
    private double temperature = 0.2D;
    private int timeoutMillis = 15000;
}
