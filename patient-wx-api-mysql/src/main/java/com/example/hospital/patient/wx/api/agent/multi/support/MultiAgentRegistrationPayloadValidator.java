package com.example.hospital.patient.wx.api.agent.multi.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class MultiAgentRegistrationPayloadValidator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[1-9]\\d*\\.\\d{1,2}$|^0\\.\\d{1,2}$|^[1-9]\\d*$");

    public ValidationResult normalizeChatPayload(Map<String, Object> payload) {
        Map<String, Object> normalized = new HashMap<>();
        Map<String, String> badFields = new LinkedHashMap<>();
        Map<String, Object> source = safeMap(payload);
        copyText(source, normalized, "deptName");
        copyText(source, normalized, "deptSubName");
        copyText(source, normalized, "doctorName");
        copyText(source, normalized, "message");
        copyText(source, normalized, "currentPage");
        copyText(source, normalized, "action");
        copyBoolean(source, normalized, "confirmed");
        copyPositiveInt(source, normalized, badFields, "deptId", null);
        copyPositiveInt(source, normalized, badFields, "deptSubId", null);
        copyPositiveInt(source, normalized, badFields, "doctorId", null);
        copyPositiveInt(source, normalized, badFields, "workPlanId", null);
        copyPositiveInt(source, normalized, badFields, "scheduleId", null);
        copyPositiveInt(source, normalized, badFields, "slot", 15);
        copyAmount(source, normalized, badFields, "amount");
        copyDate(source, normalized, badFields, "date");
        return buildResult(normalized, badFields, "挂号参数格式不正确，请重新选择号源。", "payload_invalid");
    }

    public ValidationResult normalizeConfirmationPayload(Map<String, Object> payload) {
        Map<String, Object> normalized = new HashMap<>();
        Map<String, String> badFields = new LinkedHashMap<>();
        Map<String, Object> source = safeMap(payload);
        copyBoolean(source, normalized, "confirmed");
        copyPositiveInt(source, normalized, badFields, "workPlanId", null);
        copyPositiveInt(source, normalized, badFields, "scheduleId", null);
        copyPositiveInt(source, normalized, badFields, "doctorId", null);
        copyPositiveInt(source, normalized, badFields, "deptSubId", null);
        copyPositiveInt(source, normalized, badFields, "slot", 15);
        copyAmount(source, normalized, badFields, "amount");
        copyDate(source, normalized, badFields, "date");
        return buildResult(normalized, badFields, "确认挂号参数格式不正确，请重新选择号源。", "confirmation_payload_invalid");
    }

    public ValidationResult validateExecutionOrder(Map<String, Object> payload) {
        ValidationResult base = normalizeConfirmationPayload(payload);
        Map<String, Object> normalized = new HashMap<>(base.getNormalized());
        Map<String, String> badFields = new LinkedHashMap<>(base.getBadFields());
        requireField(normalized, badFields, "workPlanId", "workPlanId不能为空");
        requireField(normalized, badFields, "scheduleId", "scheduleId不能为空");
        requireField(normalized, badFields, "doctorId", "doctorId不能为空");
        requireField(normalized, badFields, "deptSubId", "deptSubId不能为空");
        requireField(normalized, badFields, "date", "date不能为空");
        requireField(normalized, badFields, "slot", "slot不能为空");
        requireField(normalized, badFields, "amount", "amount不能为空");
        return buildResult(normalized, badFields, "挂号参数不完整或格式不正确，请重新选择号源。", "execution_payload_invalid");
    }

    public boolean matchesPendingOrder(Map<String, Object> payload, Map<String, Object> pendingOrder) {
        Map<String, Object> left = safeMap(payload);
        Map<String, Object> right = safeMap(pendingOrder);
        return sameValue(left, right, "workPlanId")
                && sameValue(left, right, "scheduleId")
                && sameValue(left, right, "doctorId")
                && sameValue(left, right, "deptSubId")
                && sameValue(left, right, "date")
                && sameValue(left, right, "slot")
                && sameAmount(left, right, "amount");
    }

    public void buildMismatchFields(Map<String, Object> payload, Map<String, Object> pendingOrder, Map<String, String> out) {
        Map<String, Object> left = safeMap(payload);
        Map<String, Object> right = safeMap(pendingOrder);
        String[] plainKeys = {"workPlanId", "scheduleId", "doctorId", "deptSubId", "date", "slot"};
        for (String key : plainKeys) {
            if (!sameValue(left, right, key)) {
                out.put(key, "期望=" + stringValue(right.get(key)) + " 实际=" + stringValue(left.get(key)));
            }
        }
        if (!sameAmount(left, right, "amount")) {
            out.put("amount", "期望=" + stringValue(right.get("amount")) + " 实际=" + stringValue(left.get("amount")));
        }
    }

    private ValidationResult buildResult(Map<String, Object> normalized,
                                         Map<String, String> badFields,
                                         String message,
                                         String badCaseType) {
        ValidationResult result = new ValidationResult();
        result.setNormalized(normalized);
        result.setBadFields(badFields);
        result.setValid(badFields.isEmpty());
        result.setMessage(message);
        result.setBadCaseType(badCaseType);
        return result;
    }

    private void requireField(Map<String, Object> normalized, Map<String, String> badFields, String key, String message) {
        if (!normalized.containsKey(key) || normalized.get(key) == null || !StringUtils.hasText(String.valueOf(normalized.get(key)))) {
            badFields.put(key, message);
        }
    }

    private void copyText(Map<String, Object> source, Map<String, Object> normalized, String key) {
        String value = stringValue(source.get(key));
        if (StringUtils.hasText(value)) {
            normalized.put(key, value.trim());
        }
    }

    private void copyBoolean(Map<String, Object> source, Map<String, Object> normalized, String key) {
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof Boolean) {
            normalized.put(key, value);
            return;
        }
        String text = stringValue(value);
        if (StringUtils.hasText(text)) {
            normalized.put(key, Boolean.parseBoolean(text.trim()));
        }
    }

    private void copyPositiveInt(Map<String, Object> source,
                                 Map<String, Object> normalized,
                                 Map<String, String> badFields,
                                 String key,
                                 Integer max) {
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        Integer parsed = parsePositiveInt(value);
        if (parsed == null || (max != null && parsed > max)) {
            badFields.put(key, key + "内容不正确");
            return;
        }
        normalized.put(key, parsed);
    }

    private void copyAmount(Map<String, Object> source,
                            Map<String, Object> normalized,
                            Map<String, String> badFields,
                            String key) {
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        String text = stringValue(value);
        if (!StringUtils.hasText(text)) {
            return;
        }
        String trimmed = text.trim();
        if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
            badFields.put(key, key + "内容不正确");
            return;
        }
        try {
            normalized.put(key, new BigDecimal(trimmed).stripTrailingZeros().toPlainString());
        } catch (Exception e) {
            badFields.put(key, key + "内容不正确");
        }
    }

    private void copyDate(Map<String, Object> source,
                          Map<String, Object> normalized,
                          Map<String, String> badFields,
                          String key) {
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        String normalizedDate = normalizeDate(value);
        if (!StringUtils.hasText(normalizedDate)) {
            badFields.put(key, key + "内容不正确");
            return;
        }
        normalized.put(key, normalizedDate);
    }

    private Integer parsePositiveInt(Object value) {
        if (value instanceof Number) {
            int number = ((Number) value).intValue();
            return number > 0 ? number : null;
        }
        String text = stringValue(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            int number = Integer.parseInt(text.trim());
            return number > 0 ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeDate(Object value) {
        String text = stringValue(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER).format(DATE_FORMATTER);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private boolean sameValue(Map<String, Object> left, Map<String, Object> right, String key) {
        Object leftValue = left.get(key);
        Object rightValue = right.get(key);
        if (leftValue == null && rightValue == null) {
            return true;
        }
        if (leftValue == null || rightValue == null) {
            return false;
        }
        return String.valueOf(leftValue).equals(String.valueOf(rightValue));
    }

    private boolean sameAmount(Map<String, Object> left, Map<String, Object> right, String key) {
        Object leftValue = left.get(key);
        Object rightValue = right.get(key);
        if (leftValue == null && rightValue == null) {
            return true;
        }
        if (leftValue == null || rightValue == null) {
            return false;
        }
        try {
            return new BigDecimal(String.valueOf(leftValue)).compareTo(new BigDecimal(String.valueOf(rightValue))) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<String, Object>() : map;
    }

    public static class ValidationResult {
        private boolean valid;
        private String message;
        private String badCaseType;
        private Map<String, Object> normalized;
        private Map<String, String> badFields;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getBadCaseType() {
            return badCaseType;
        }

        public void setBadCaseType(String badCaseType) {
            this.badCaseType = badCaseType;
        }

        public Map<String, Object> getNormalized() {
            return normalized;
        }

        public void setNormalized(Map<String, Object> normalized) {
            this.normalized = normalized;
        }

        public Map<String, String> getBadFields() {
            return badFields;
        }

        public void setBadFields(Map<String, String> badFields) {
            this.badFields = badFields;
        }
    }
}
