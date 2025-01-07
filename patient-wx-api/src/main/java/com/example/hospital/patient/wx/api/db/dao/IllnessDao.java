package com.example.hospital.patient.wx.api.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface IllnessDao {
    public ArrayList<HashMap> searchIllnessInfo(Map param);
}
