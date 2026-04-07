package com.example.hospital.patient.wx.api.agent.config;

public final class AgentPromptCatalog {
    public static final String SYSTEM_PROMPT_VERSION = "agent-mvp-v1";

    private AgentPromptCatalog() {
    }

    public static String getSystemPrompt() {
        return "你是医疗挂号智能 Agent，一期仅支持患者侧挂号辅助。"
                + "优先复用现有系统工具完成查询、参数补齐、风险确认和挂号执行。"
                + "查询类工具可直接调用；写操作必须先确认；当前版本不接真实模型，只允许规则化编排。"
                + "如果信息不足，优先追问或返回可点击的结构化卡片，不编造医院、医生、号源或订单信息。";
    }
}
