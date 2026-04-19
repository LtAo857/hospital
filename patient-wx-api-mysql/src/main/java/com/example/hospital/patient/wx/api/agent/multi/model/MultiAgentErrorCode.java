package com.example.hospital.patient.wx.api.agent.multi.model;

public final class MultiAgentErrorCode {
    public static final String REGISTRATION_LOGIN_REQUIRED = "REGISTRATION_LOGIN_REQUIRED";
    public static final String REGISTRATION_USER_CARD_REQUIRED = "REGISTRATION_USER_CARD_REQUIRED";
    public static final String REGISTRATION_DUPLICATE_SUBMIT = "REGISTRATION_DUPLICATE_SUBMIT";
    public static final String REGISTRATION_DAILY_LIMIT_REACHED = "REGISTRATION_DAILY_LIMIT_REACHED";
    public static final String REGISTRATION_REPEAT_IN_DAY = "REGISTRATION_REPEAT_IN_DAY";
    public static final String REGISTRATION_SLOT_EXHAUSTED = "REGISTRATION_SLOT_EXHAUSTED";
    public static final String REGISTRATION_SLOT_CHANGED = "REGISTRATION_SLOT_CHANGED";
    public static final String REGISTRATION_PARAM_MISMATCH = "REGISTRATION_PARAM_MISMATCH";
    public static final String REGISTRATION_DB_WRITE_FAILED = "REGISTRATION_DB_WRITE_FAILED";
    public static final String REGISTRATION_SYSTEM_ERROR = "REGISTRATION_SYSTEM_ERROR";

    private MultiAgentErrorCode() {
    }
}
