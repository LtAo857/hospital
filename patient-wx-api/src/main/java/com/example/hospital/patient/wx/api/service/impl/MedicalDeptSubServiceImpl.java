package com.example.hospital.patient.wx.api.service.impl;

import com.example.hospital.patient.wx.api.db.dao.MedicalDeptSubDao;
import com.example.hospital.patient.wx.api.service.MedicalDeptSubService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class MedicalDeptSubServiceImpl implements MedicalDeptSubService {
    @Resource
    private MedicalDeptSubDao medicalDeptSubDao;

    @Override
    public ArrayList<HashMap> searchMedicalDeptSubList(int deptId) {
        ArrayList<HashMap> list = medicalDeptSubDao.searchMedicalDeptSubList(deptId);
        return list;
    }

}