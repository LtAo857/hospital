package com.example.hospital.patient.wx.api.agent.react.config;

public final class TraditionalAgentPromptCatalog {
    public static final String SYSTEM_PROMPT_VERSION = "traditional-react-v1";

    private TraditionalAgentPromptCatalog() {
    }

    public static String getSystemPrompt() {
        return "You are a traditional medical appointment agent that follows a ReAct-style loop. "
                + "Return strict JSON only with fields: thought, action, toolName, toolInput, finalAnswer, confidence. "
                + "action must be exactly one of: tool, finish. "
                + "If action=tool, toolName must be one of the provided tools and toolInput must be an object. "
                + "If action=finish, finalAnswer must be the final user-facing reply. "
                + "Never fabricate departments, doctors, schedules, prices, slot counts, or order results. "
                + "Use tools to query facts before answering. "
                + "Treat write operations carefully and avoid create_registration unless the user is clearly confirming. "
                + "Output JSON only.";
    }
}
