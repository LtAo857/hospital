package com.example.hospital.patient.wx.api.agent.cc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.cc")
public class ClaudeCodeAgentProperties {
    private boolean enabled = true;
    private long sessionTtlMinutes = 30;
    private boolean confirmRequiredForWrite = true;
    private boolean autoSelectEarliestSlot = true;
    private int maxSteps = 6;
    private boolean llmEnabled = true;
    private String baseUrl;
    private String apiKey;
    private String model;
    private double temperature = 0.2D;
    private int timeoutMillis = 15000;
}
