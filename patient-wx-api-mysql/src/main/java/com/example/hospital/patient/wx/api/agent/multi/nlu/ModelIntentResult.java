package com.example.hospital.patient.wx.api.agent.multi.nlu;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ModelIntentResult {
    private String intent;
    private Map<String, Object> slots = new HashMap<>();
    private double confidence;
    private String source;
    private String engine;
    private String model;
    private Long latencyMs;
}

