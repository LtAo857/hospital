package com.example.hospital.patient.wx.api.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    private boolean enabled = true;
    private long sessionTtlMinutes = 30;
    private boolean confirmRequiredForWrite = true;
    private boolean mockModelEnabled = false;
}
