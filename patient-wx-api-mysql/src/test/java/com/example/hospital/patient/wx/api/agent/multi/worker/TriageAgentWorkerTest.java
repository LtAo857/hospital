package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

class TriageAgentWorkerTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    void shouldRouteToSlotQueryWhenRegistrationIntentDetected() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("我明天想挂号");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals(
                LocalDate.now().plusDays(1).format(DATE_FORMATTER),
                result.getMemoryPatch().get("date")
        );
    }

    @Test
    void shouldRouteToSlotQueryWhenOnlyDeptAndDateProvided() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("明天的骨科");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals(
                LocalDate.now().plusDays(1).format(DATE_FORMATTER),
                result.getMemoryPatch().get("date")
        );
    }
}
