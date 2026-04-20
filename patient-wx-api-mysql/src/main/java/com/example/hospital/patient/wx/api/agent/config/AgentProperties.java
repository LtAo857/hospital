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
    private boolean llmEnabled = false;
    private String provider = "dashscope";
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private String apiKey;
    private String model = "qwen-plus";
    private double temperature = 0.2D;
    private int timeoutMillis = 15000;
    private int httpRetryCount = 1;
    private int maxPromptChars = 1200;
}
