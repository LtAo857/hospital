package com.example.hospital.patient.wx.api.agent.multi.rag;

import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MultiAgentRagServiceTest {

    @Test
    void shouldSanitizeMemoryAndReturnSnippetAnswer() {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.setLlmEnabled(false);
        MultiAgentProperties multiAgentProperties = new MultiAgentProperties();
        multiAgentProperties.setRagEmbeddingEnabled(false);
        MultiAgentKnowledgeBase knowledgeBase = new MultiAgentKnowledgeBase(multiAgentProperties, null);
        MultiAgentRagService ragService = new MultiAgentRagService(agentProperties, multiAgentProperties, knowledgeBase);

        Map<String, Object> memory = new HashMap<String, Object>();
        memory.put("deptSubName", "口腔门诊");
        memory.put("doctorName", "张医生");
        memory.put("idCard", "123456789012345678");
        memory.put("pendingOrder", new HashMap<String, Object>() {{
            put("date", "2026-04-20");
            put("slot", 1);
            put("secret", "hidden");
        }});

        MultiAgentRagService.RagAnswer answer = ragService.answer("为什么推荐这个", memory);

        Assertions.assertNotNull(answer);
        Assertions.assertNotNull(answer.getSafeMemory());
        Assertions.assertNull(answer.getSafeMemory().get("idCard"));
        Assertions.assertTrue(answer.getSafeMemory().containsKey("pendingOrder"));
        Assertions.assertTrue(answer.getHitCount() >= 1);
        Assertions.assertTrue(answer.getLatencyMs() >= 0);
    }

    @Test
    void shouldFallbackWhenNoSnippetFound() {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.setLlmEnabled(false);
        MultiAgentProperties multiAgentProperties = new MultiAgentProperties();
        multiAgentProperties.setRagEmbeddingEnabled(false);
        MultiAgentKnowledgeBase knowledgeBase = new MultiAgentKnowledgeBase(multiAgentProperties, null) {
            @Override
            public SearchResult retrieve(String query, int limit) {
                return SearchResult.empty("fallback", "no_hit");
            }
        };
        MultiAgentRagService ragService = new MultiAgentRagService(agentProperties, multiAgentProperties, knowledgeBase);

        MultiAgentRagService.RagAnswer answer = ragService.answer("abcxyz12345", new HashMap<String, Object>());

        Assertions.assertNotNull(answer);
        Assertions.assertEquals("fallback", answer.getMode());
        Assertions.assertEquals(0, answer.getHitCount());
        Assertions.assertEquals("no_hit", answer.getFallbackReason());
        Assertions.assertTrue(answer.getAnswer().contains("解释能力") || answer.getAnswer().contains("推荐"));
    }
}
