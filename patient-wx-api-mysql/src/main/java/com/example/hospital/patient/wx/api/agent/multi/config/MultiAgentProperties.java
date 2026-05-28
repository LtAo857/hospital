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
    private boolean ragEmbeddingEnabled = true;
    private String ragEmbeddingProvider = "local";
    private String ragEmbeddingBaseUrl;
    private String ragEmbeddingApiKey;
    private String ragEmbeddingModel = "local-hash";
    private int ragEmbeddingDimensions = 64;
    private int ragKeywordTopK = 3;
    private int ragVectorTopK = 3;
    private double ragVectorScoreThreshold = 0.08D;
    private long ragQueryCacheMinutes = 15;
    private int ragMaxContextChars = 600;
    private int ragHttpRetryCount = 1;
    private int guardRequestLimitPerMinute = 12;
    private long guardMinIntervalMillis = 1000L;
    private boolean modelParserEnabled = false;
    private String modelParserEndpoint = "http://127.0.0.1:8001/infer";
    private int modelParserTimeoutMillis = 800;
    private double modelParserMinConfidence = 0.75D;
}
