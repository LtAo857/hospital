package com.example.hospital.patient.wx.api.agent.multi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.multi")
public class MultiAgentProperties {
    private boolean enabled = true;
    private long sessionTtlMinutes = 30;
    private int maxHops = 8;
}
