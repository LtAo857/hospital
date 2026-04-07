package com.example.hospital.patient.wx.api.agent.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPlanStep {
    private String key;
    private String label;
    private String status;
}
