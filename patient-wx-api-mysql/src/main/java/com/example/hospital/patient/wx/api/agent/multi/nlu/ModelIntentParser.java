package com.example.hospital.patient.wx.api.agent.multi.nlu;

import java.util.Optional;

public interface ModelIntentParser {
    Optional<ModelIntentResult> parse(String text, String sessionId);
}

