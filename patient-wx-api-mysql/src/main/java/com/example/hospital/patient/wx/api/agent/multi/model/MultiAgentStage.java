package com.example.hospital.patient.wx.api.agent.multi.model;

public enum MultiAgentStage {
    INTENT_PARSE,
    SLOT_QUERY,
    POLICY_CHECK,
    CONFIRM_WAIT,
    EXECUTE_APPOINTMENT,
    DONE,
    MANUAL_FALLBACK
}
