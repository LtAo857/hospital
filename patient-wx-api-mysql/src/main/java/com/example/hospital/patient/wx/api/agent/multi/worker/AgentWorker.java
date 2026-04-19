package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;

public interface AgentWorker {
    MultiAgentStage stage();

    AgentResult execute(AgentContext context);
}
