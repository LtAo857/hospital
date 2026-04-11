package com.example.hospital.patient.wx.api.agent.cc.config;

public final class ClaudeCodeAgentPromptCatalog {
    public static final String SYSTEM_PROMPT_VERSION = "claude-code-v1";

    private ClaudeCodeAgentPromptCatalog() {
    }

    public static String getSystemPrompt() {
        return "CC architecture: no ReAct thoughts, plan first, execute grounded tools, "
                + "then end turn with a concise user reply. Only real tool data may drive department, "
                + "doctor, schedule, slot, and registration decisions. Read tools can auto-chain, "
                + "write tools always require confirmation.";
    }
}
