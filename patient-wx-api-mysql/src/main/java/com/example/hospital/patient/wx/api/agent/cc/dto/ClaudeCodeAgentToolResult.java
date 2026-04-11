package com.example.hospital.patient.wx.api.agent.cc.dto;

import com.example.hospital.patient.wx.api.agent.dto.AgentResponseCard;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ClaudeCodeAgentToolResult {
    private boolean success = true;
    private String summary;
    private Object observation;
    private Map<String, Object> memoryUpdates = new HashMap<>();
    private boolean terminal;
    private String terminalReply;
    private String terminalState;
    private List<AgentResponseCard> cards = new ArrayList<>();
}
