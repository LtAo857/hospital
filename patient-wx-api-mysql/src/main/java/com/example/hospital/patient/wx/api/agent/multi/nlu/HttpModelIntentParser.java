package com.example.hospital.patient.wx.api.agent.multi.nlu;

import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpModelIntentParser implements ModelIntentParser {
    private static final Logger log = LoggerFactory.getLogger(HttpModelIntentParser.class);

    private final MultiAgentProperties properties;
    private final ObjectMapper objectMapper;

    public HttpModelIntentParser(MultiAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        log.info("HttpModelIntentParser created, enabled={}, endpoint={}",
                properties.isModelParserEnabled(), properties.getModelParserEndpoint());
    }

    @Override
    public Optional<ModelIntentResult> parse(String text, String sessionId, List<String> departments) {
        log.info("NLU parse called, enabled={}, text={}",
                properties.isModelParserEnabled(), text != null ? text.substring(0, Math.min(30, text.length())) : "null");
        if (!properties.isModelParserEnabled() || !StringUtils.hasText(text) || !StringUtils.hasText(properties.getModelParserEndpoint())) {
            log.info("NLU parse skipped: enabled={}, hasText={}, hasEndpoint={}",
                    properties.isModelParserEnabled(),
                    StringUtils.hasText(text),
                    StringUtils.hasText(properties.getModelParserEndpoint()));
            return Optional.empty();
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(properties.getModelParserEndpoint()).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getModelParserTimeoutMillis());
            connection.setReadTimeout(properties.getModelParserTimeoutMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            Map<String, Object> request = new HashMap<>();
            request.put("text", text);
            if (StringUtils.hasText(sessionId)) {
                request.put("sessionId", sessionId);
            }
            if (departments != null && !departments.isEmpty()) {
                request.put("departments", departments);
            }
            byte[] body = objectMapper.writeValueAsBytes(request);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("NLU HTTP status={}", status);
                return Optional.empty();
            }
            ModelIntentResult result = objectMapper.readValue(readAll(connection.getInputStream()), ModelIntentResult.class);
            log.info("NLU result: intent={}, confidence={}, source={}",
                    result.getIntent(), result.getConfidence(), result.getSource());
            if (result.getConfidence() < properties.getModelParserMinConfidence()) {
                log.info("NLU confidence {} below threshold {}, ignored",
                        result.getConfidence(), properties.getModelParserMinConfidence());
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("NLU HTTP call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private byte[] readAll(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }
}

