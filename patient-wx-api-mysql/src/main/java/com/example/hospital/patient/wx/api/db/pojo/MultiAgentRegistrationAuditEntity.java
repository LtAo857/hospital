package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class MultiAgentRegistrationAuditEntity {
    private Integer id;
    private String sessionId;
    private String requestId;
    private Integer userId;
    private Integer workPlanId;
    private Integer scheduleId;
    private Integer doctorId;
    private Integer deptSubId;
    private String date;
    private Integer slot;
    private BigDecimal amount;
    private String outTradeNo;
    private String status;
    private String errorCode;
    private String errorMessage;
    private Integer registrationId;
    private String traceJson;
    private Date createdAt;
    private Date updatedAt;
}
