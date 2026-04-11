package com.example.hospital.patient.wx.api.agent.cc.service;

import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.support.AgentPlanStep;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeCodeAgentPlannerService {
    public String enterPlan(AgentChatRequest request, Map<String, Object> runtime) {
        String planName = isRegistrationContext(request, runtime) ? "registration_plan" : "assist_plan";
        runtime.put("planMode", true);
        runtime.put("currentPlan", planName);
        return planName;
    }

    public void exitPlan(Map<String, Object> runtime, String status) {
        runtime.put("planStatus", status);
        runtime.put("planMode", false);
    }

    public List<AgentPlanStep> buildSteps(Map<String, Object> runtime, String state) {
        List<AgentPlanStep> steps = new ArrayList<>();
        boolean hasPlan = runtime.get("currentPlan") != null;
        boolean hasObservation = runtime.get("lastObservation") != null;
        boolean awaitingConfirmation = "awaiting_confirmation".equals(state);
        steps.add(new AgentPlanStep("memory", "加载记忆", "completed"));
        steps.add(new AgentPlanStep("plan", "进入计划", hasPlan ? "completed" : "pending"));
        steps.add(new AgentPlanStep("execute", "串行执行", hasObservation ? "completed" : "in_progress"));
        steps.add(new AgentPlanStep("confirm", "确认写操作", awaitingConfirmation ? "in_progress" : "completed".equals(state) ? "completed" : "pending"));
        return steps;
    }

    private boolean isRegistrationContext(AgentChatRequest request, Map<String, Object> runtime) {
        if (runtime.get("deptId") != null || runtime.get("deptName") != null || runtime.get("doctorId") != null || runtime.get("date") != null) {
            return true;
        }
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            return false;
        }
        String message = request.getMessage();
        return message.contains("挂号") || message.contains("预约") || message.contains("号");
    }
}
