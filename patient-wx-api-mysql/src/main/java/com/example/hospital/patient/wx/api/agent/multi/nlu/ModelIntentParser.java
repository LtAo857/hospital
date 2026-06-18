package com.example.hospital.patient.wx.api.agent.multi.nlu;

import java.util.List;
import java.util.Optional;

public interface ModelIntentParser {
    Optional<ModelIntentResult> parse(String text, String sessionId, List<String> departments);
}

