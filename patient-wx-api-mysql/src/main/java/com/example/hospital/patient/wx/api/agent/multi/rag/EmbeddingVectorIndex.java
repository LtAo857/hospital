package com.example.hospital.patient.wx.api.agent.multi.rag;

import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmbeddingVectorIndex {
    @Resource
    private MultiAgentProperties properties;

    @Resource
    private EmbeddingClient embeddingClient;

    private final Map<String, double[]> documentVectorCache = new ConcurrentHashMap<String, double[]>();

    public EmbeddingVectorIndex() {
        this.properties = new MultiAgentProperties();
        this.embeddingClient = new EmbeddingClient(this.properties);
    }

    public EmbeddingVectorIndex(MultiAgentProperties properties,
                                EmbeddingClient embeddingClient) {
        this.properties = properties == null ? new MultiAgentProperties() : properties;
        this.embeddingClient = embeddingClient == null ? new EmbeddingClient(this.properties) : embeddingClient;
    }

    public List<RetrievalHit> search(String query,
                                     List<MultiAgentKnowledgeBase.KnowledgeSnippet> corpus,
                                     String corpusVersion,
                                     int limit) {
        MultiAgentProperties current = properties == null ? new MultiAgentProperties() : properties;
        if (!current.isRagEmbeddingEnabled() || !StringUtils.hasText(query) || corpus == null || corpus.isEmpty()) {
            return Collections.emptyList();
        }
        EmbeddingClient.EmbeddingResponse queryEmbedding = embeddingClient == null ? null : embeddingClient.embed(query);
        if (queryEmbedding == null || queryEmbedding.getVector().length == 0) {
            return Collections.emptyList();
        }
        List<RetrievalHit> hits = new ArrayList<RetrievalHit>();
        for (MultiAgentKnowledgeBase.KnowledgeSnippet snippet : corpus) {
            if (snippet == null || !StringUtils.hasText(snippet.getContent())) {
                continue;
            }
            double[] vector = resolveVector(snippet, corpusVersion);
            if (vector.length == 0) {
                continue;
            }
            double score = cosine(queryEmbedding.getVector(), vector);
            if (score < current.getRagVectorScoreThreshold()) {
                continue;
            }
            RetrievalHit hit = new RetrievalHit();
            hit.setSnippet(snippet);
            hit.setVectorScore(score);
            hit.setFinalScore(score * 10D);
            hit.setMode("vector");
            hits.add(hit);
        }
        Collections.sort(hits, new Comparator<RetrievalHit>() {
            @Override
            public int compare(RetrievalHit left, RetrievalHit right) {
                return Double.compare(right.getFinalScore(), left.getFinalScore());
            }
        });
        int size = Math.min(limit <= 0 ? current.getRagVectorTopK() : limit, hits.size());
        List<RetrievalHit> result = new ArrayList<RetrievalHit>();
        for (int i = 0; i < size; i++) {
            result.add(hits.get(i));
        }
        return result;
    }

    private double[] resolveVector(MultiAgentKnowledgeBase.KnowledgeSnippet snippet, String corpusVersion) {
        String cacheKey = (StringUtils.hasText(corpusVersion) ? corpusVersion : "default") + ":" + snippet.getCacheKey();
        double[] cached = documentVectorCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        EmbeddingClient.EmbeddingResponse embedding = embeddingClient == null ? null : embeddingClient.embed(snippet.getTitle() + " " + snippet.getContent());
        double[] vector = embedding == null ? new double[0] : embedding.getVector();
        documentVectorCache.put(cacheKey, vector);
        return vector;
    }

    private double cosine(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            return 0D;
        }
        int size = Math.min(left.length, right.length);
        double score = 0D;
        for (int i = 0; i < size; i++) {
            score += left[i] * right[i];
        }
        return score;
    }
}
