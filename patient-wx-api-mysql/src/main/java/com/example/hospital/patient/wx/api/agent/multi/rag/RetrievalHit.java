package com.example.hospital.patient.wx.api.agent.multi.rag;

import lombok.Data;

@Data
public class RetrievalHit {
    private MultiAgentKnowledgeBase.KnowledgeSnippet snippet;
    private double keywordScore;
    private double vectorScore;
    private double finalScore;
    private String mode;
}
