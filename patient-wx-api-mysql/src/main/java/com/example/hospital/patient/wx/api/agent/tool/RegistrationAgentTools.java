package com.example.hospital.patient.wx.api.agent.tool;

import com.example.hospital.patient.wx.api.service.RegistrationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class RegistrationAgentTools {
    @Resource
    private RegistrationService registrationService;

    @SuppressWarnings("unchecked")
    public ArrayList<HashMap> searchRegisterDates(int deptSubId, String startDate, String endDate) {
        Map<String, Object> param = new HashMap<>();
        param.put("deptSubId", deptSubId);
        param.put("startDate", startDate);
        param.put("endDate", endDate);
        ArrayList result = registrationService.searchCanRegisterInDateRange(param);
        return (ArrayList<HashMap>) result;
    }

    public ArrayList<HashMap> searchDoctorPlansInDay(int deptSubId, String date) {
        Map<String, Object> param = new HashMap<>();
        param.put("deptSubId", deptSubId);
        param.put("date", date);
        return registrationService.searchDeptSubDoctorPlanInDay(param);
    }

    public ArrayList<HashMap> searchScheduleSlots(int doctorId, String date) {
        Map<String, Object> param = new HashMap<>();
        param.put("doctorId", doctorId);
        param.put("date", date);
        return registrationService.searchDoctorWorkPlanSchedule(param);
    }

    public String checkRegistrationCondition(int userId, int deptSubId, String date) {
        Map<String, Object> param = new HashMap<>();
        param.put("userId", userId);
        param.put("deptSubId", deptSubId);
        param.put("date", date);
        return registrationService.checkRegisterCondition(param);
    }

    public HashMap createRegistrationOrder(int userId, Map<String, Object> payload) {
        Map<String, Object> param = new HashMap<>(payload);
        param.put("userId", userId);
        return registrationService.registerMedicalAppointment(param);
    }
}
