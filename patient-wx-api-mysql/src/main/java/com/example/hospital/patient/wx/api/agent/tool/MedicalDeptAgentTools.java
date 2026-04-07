package com.example.hospital.patient.wx.api.agent.tool;

import com.example.hospital.patient.wx.api.service.MedicalDeptService;
import com.example.hospital.patient.wx.api.service.MedicalDeptSubService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class MedicalDeptAgentTools {
    @Resource
    private MedicalDeptService medicalDeptService;

    @Resource
    private MedicalDeptSubService medicalDeptSubService;

    public ArrayList<HashMap> searchDepartments(Boolean recommended, Boolean outpatient) {
        Map<String, Object> param = new HashMap<>();
        param.put("recommended", recommended);
        param.put("outpatient", outpatient);
        return medicalDeptService.searchMedicalDeptList(param);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, ArrayList<HashMap>> searchDepartmentTree() {
        HashMap<String, ArrayList<HashMap>> result = new HashMap<>();
        HashMap data = medicalDeptService.searchDeptAndSub();
        for (Object key : data.keySet()) {
            result.put(String.valueOf(key), (ArrayList<HashMap>) data.get(key));
        }
        return result;
    }

    public ArrayList<HashMap> searchSubDepartments(int deptId) {
        return medicalDeptSubService.searchMedicalDeptSubList(deptId);
    }
}
