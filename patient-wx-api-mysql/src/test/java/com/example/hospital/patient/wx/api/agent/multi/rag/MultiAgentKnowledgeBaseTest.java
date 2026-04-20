package com.example.hospital.patient.wx.api.agent.multi.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class MultiAgentKnowledgeBaseTest {

    @Test
    void shouldLoadRecommendationSnippetsFromDocs() {
        MultiAgentKnowledgeBase knowledgeBase = new MultiAgentKnowledgeBase();

        List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets = knowledgeBase.search("为什么推荐这个", 3);

        Assertions.assertFalse(snippets.isEmpty());
        Assertions.assertTrue(snippets.stream().anyMatch(snippet -> snippet.getTitle().contains("Multi-Agent 实现说明")));
        Assertions.assertTrue(snippets.stream().anyMatch(snippet -> snippet.getContent().contains("完整思考链") || snippet.getContent().contains("候选号源") || snippet.getContent().contains("推荐")));
    }

    @Test
    void shouldLoadRegistrationCapabilitySnippetsFromDocs() {
        MultiAgentKnowledgeBase knowledgeBase = new MultiAgentKnowledgeBase();

        List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets = knowledgeBase.search("查看我的挂号和消息", 3);

        Assertions.assertFalse(snippets.isEmpty());
        Assertions.assertTrue(snippets.stream().anyMatch(snippet -> snippet.getContent().contains("查看我的挂号") || snippet.getContent().contains("消息中心") || snippet.getContent().contains("消息")));
    }
}
