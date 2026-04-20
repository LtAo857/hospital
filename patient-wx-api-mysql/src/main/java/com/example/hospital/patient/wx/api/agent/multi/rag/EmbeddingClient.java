package com.example.hospital.patient.wx.api.agent.multi.rag;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class EmbeddingClient {
    @Resource
    private MultiAgentProperties properties;

    public EmbeddingClient() {
        this.properties = new MultiAgentProperties();
    }

    public EmbeddingClient(MultiAgentProperties properties) {
        this.properties = properties == null ? new MultiAgentProperties() : properties;
    }

    public EmbeddingResponse embed(String text) {
        MultiAgentProperties current = properties == null ? new MultiAgentProperties() : properties;
        if (!StringUtils.hasText(text) || !current.isRagEmbeddingEnabled()) {
            return null;
        }
        String provider = StringUtils.hasText(current.getRagEmbeddingProvider()) ? current.getRagEmbeddingProvider() : "local";
        if ("http".equalsIgnoreCase(provider) && StringUtils.hasText(current.getRagEmbeddingBaseUrl())) {
            EmbeddingResponse remote = embedByHttp(text, current);
            if (remote != null && remote.getVector() != null && remote.getVector().length > 0) {
                return remote;
            }
        }
        return embedLocally(text, current);
    }

    private EmbeddingResponse embedByHttp(String text, MultiAgentProperties current) {
        int attempts = Math.max(0, current.getRagHttpRetryCount()) + 1;
        RuntimeException lastError = null;
        for (int i = 0; i < attempts; i++) {
            try {
                JSONObject body = new JSONObject();
                body.set("model", current.getRagEmbeddingModel());
                body.set("input", text);
                HttpResponse response = HttpRequest.post(current.getRagEmbeddingBaseUrl())
                        .header("Authorization", StringUtils.hasText(current.getRagEmbeddingApiKey()) ? "Bearer " + current.getRagEmbeddingApiKey() : null)
                        .header("Content-Type", "application/json")
                        .timeout(10000)
                        .body(body.toString())
                        .execute();
                if (response.getStatus() >= 200 && response.getStatus() < 300) {
                    JSONObject root = JSONUtil.parseObj(response.body());
                    JSONArray data = root.getJSONArray("data");
                    if (data == null || data.isEmpty()) {
                        return null;
                    }
                    JSONObject first = data.getJSONObject(0);
                    JSONArray embedding = first == null ? null : first.getJSONArray("embedding");
                    if (embedding == null || embedding.isEmpty()) {
                        return null;
                    }
                    double[] vector = new double[embedding.size()];
                    for (int j = 0; j < embedding.size(); j++) {
                        vector[j] = embedding.getDouble(j);
                    }
                    normalize(vector);
                    JSONObject usage = root.getJSONObject("usage");
                    int promptTokens = usage == null ? estimateTokens(text) : usage.getInt("prompt_tokens", estimateTokens(text));
                    return new EmbeddingResponse(vector, "http", promptTokens);
                }
                if (!isRetryableStatus(response.getStatus()) || i == attempts - 1) {
                    return null;
                }
            } catch (RuntimeException e) {
                lastError = e;
                if (i == attempts - 1) {
                    break;
                }
            }
        }
        if (lastError != null) {
            return null;
        }
        return null;
    }

    private EmbeddingResponse embedLocally(String text, MultiAgentProperties current) {
        int dims = current.getRagEmbeddingDimensions() <= 0 ? 64 : current.getRagEmbeddingDimensions();
        double[] vector = new double[dims];
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return new EmbeddingResponse(vector, "local", 0);
        }
        for (int i = 0; i < normalized.length(); i++) {
            addToken(vector, String.valueOf(normalized.charAt(i)));
            if (i + 1 < normalized.length()) {
                addToken(vector, normalized.substring(i, i + 2));
            }
            if (i + 2 < normalized.length()) {
                addToken(vector, normalized.substring(i, i + 3));
            }
        }
        normalize(vector);
        return new EmbeddingResponse(vector, "local", estimateTokens(normalized));
    }

    private void addToken(double[] vector, String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        int index = Math.abs(token.hashCode()) % vector.length;
        vector[index] += 1D;
    }

    private void normalize(double[] vector) {
        double sum = 0D;
        for (double value : vector) {
            sum += value * value;
        }
        if (sum <= 0D) {
            return;
        }
        double base = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / base;
        }
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace("`", "")
                .replace("*", "")
                .replace("_", "")
                .replace("#", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    public static class EmbeddingResponse {
        private final double[] vector;
        private final String provider;
        private final int promptTokens;

        public EmbeddingResponse(double[] vector, String provider, int promptTokens) {
            this.vector = vector == null ? new double[0] : Arrays.copyOf(vector, vector.length);
            this.provider = provider;
            this.promptTokens = promptTokens;
        }

        public double[] getVector() {
            return Arrays.copyOf(vector, vector.length);
        }

        public String getProvider() {
            return provider;
        }

        public int getPromptTokens() {
            return promptTokens;
        }
    }
}
