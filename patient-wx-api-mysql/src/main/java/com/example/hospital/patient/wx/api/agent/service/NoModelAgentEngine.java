package com.example.hospital.patient.wx.api.agent.service;

import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.support.AgentUiAction;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class NoModelAgentEngine {
    public String resolveAction(AgentChatRequest request, Map<String, Object> memory) {
        if (request != null && StringUtils.hasText(request.getAction())) {
            return request.getAction();
        }

        String stage = stringValue(memory.get("stage"));
        if (StringUtils.hasText(stage)) {
            if ("choose_department".equals(stage)) {
                return AgentUiAction.VIEW_DEPARTMENTS;
            }
            if ("choose_sub_department".equals(stage) && memory.get("deptId") != null) {
                return AgentUiAction.SELECT_SUB_DEPT;
            }
            if ("choose_date".equals(stage) && memory.get("deptSubId") != null) {
                return AgentUiAction.SELECT_DATE;
            }
            if ("choose_doctor".equals(stage) && memory.get("date") != null) {
                return AgentUiAction.SELECT_DOCTOR;
            }
            if ("choose_slot".equals(stage) && memory.get("doctorId") != null) {
                return AgentUiAction.SELECT_SLOT;
            }
            if ("awaiting_confirmation".equals(stage) && memory.get("pendingOrder") != null) {
                return AgentAction.CREATE_REGISTRATION;
            }
        }

        String message = request == null ? null : request.getMessage();
        if (!StringUtils.hasText(message)) {
            return AgentUiAction.WELCOME;
        }
        if (containsAny(message, "消息", "通知", "提醒")) {
            return AgentUiAction.VIEW_MESSAGES;
        }
        if (containsAny(message, "就诊卡", "实名", "身份信息")) {
            return AgentUiAction.VIEW_USER_CARD;
        }
        if (containsAny(message, "挂号", "预约", "科室", "诊室", "医生", "号源", "口腔", "牙")) {
            return AgentUiAction.START_REGISTRATION;
        }
        return AgentUiAction.WELCOME;
    }

    public boolean isWriteAction(String action) {
        return AgentAction.CREATE_REGISTRATION.equals(action);
    }

    public String fallbackReply() {
        return "我目前可以帮你完成科室、医生、号源、消息和挂号确认。你可以点下方卡片继续。";
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
