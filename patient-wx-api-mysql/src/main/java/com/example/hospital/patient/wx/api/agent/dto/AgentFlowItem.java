package com.example.hospital.patient.wx.api.agent.dto;

import lombok.Data;

@Data
public class AgentFlowItem {
    private String key;
    private String title;
    private String stage;
    private String status;
    private String summary;
    private String handoffAction;
    private Integer toolCount;
}
