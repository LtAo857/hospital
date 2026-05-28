package com.example.hospital.patient.wx.api.agent.multi.nlu;

import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpModelIntentParser implements ModelIntentParser {
    private final MultiAgentProperties properties;
    private final ObjectMapper objectMapper;

    public HttpModelIntentParser(MultiAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ModelIntentResult> parse(String text, String sessionId) {
        if (!properties.isModelParserEnabled() || !StringUtils.hasText(text) || !StringUtils.hasText(properties.getModelParserEndpoint())) {
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
            byte[] body = objectMapper.writeValueAsBytes(request);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return Optional.empty();
            }
            ModelIntentResult result = objectMapper.readValue(readAll(connection.getInputStream()), ModelIntentResult.class);
            if (result.getConfidence() < properties.getModelParserMinConfidence()) {
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (Exception ignored) {
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

