package com.example.hospital.patient.wx.api.agent.multi.rag;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class MultiAgentRagEvaluationTest {

    @Test
    void shouldHitExpectedTitlesOnOfflineCases() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setRagEmbeddingEnabled(false);
        MultiAgentKnowledgeBase knowledgeBase = new MultiAgentKnowledgeBase(properties, null);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("agent/multi/rag_eval_cases.json");
        Assertions.assertNotNull(stream);
        String json = IoUtil.read(stream, StandardCharsets.UTF_8);
        JSONArray array = JSONUtil.parseArray(json);
        int hit = 0;
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            MultiAgentKnowledgeBase.SearchResult result = knowledgeBase.retrieve(item.getStr("question"), 3);
            Assertions.assertFalse(result.getSnippets().isEmpty());
            if (result.getSnippets().get(0).getTitle().contains(item.getStr("expectedTitle"))) {
                hit++;
            }
        }
        Assertions.assertTrue(hit >= 2);
    }
}
