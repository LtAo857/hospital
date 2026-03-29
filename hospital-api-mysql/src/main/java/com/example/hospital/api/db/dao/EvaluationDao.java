package com.example.hospital.api.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface EvaluationDao {
    long searchCount(Map param);

    ArrayList<HashMap> searchByPage(Map param);
}
