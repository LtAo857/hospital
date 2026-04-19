package com.example.hospital.patient.wx.api.db.dao;

import com.example.hospital.patient.wx.api.db.pojo.MultiAgentRegistrationAuditEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MultiAgentRegistrationAuditDao {
    int insert(MultiAgentRegistrationAuditEntity entity);

    HashMap searchByRequestId(String requestId);

    int updateByRequestId(Map param);

    ArrayList<HashMap> searchRepairCandidatesBeforeMinutes(int minutes);
}
