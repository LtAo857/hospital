package com.example.hospital.api.service.impl;

import com.example.hospital.api.db.dao.PatientDao;
import com.example.hospital.api.db.dao.PetDao;
import com.example.hospital.api.service.PatientService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class PatientServiceImpl implements PatientService {
    @Resource
    private PatientDao patientDao;
    @Override
    public ArrayList<HashMap> searchAllPatient() {
        return patientDao.searchAllPatient();
    }
}
