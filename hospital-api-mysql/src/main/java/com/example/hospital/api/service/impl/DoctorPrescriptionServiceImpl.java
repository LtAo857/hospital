package com.example.hospital.api.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.dao.DoctorPrescriptionDao;
import com.example.hospital.api.db.dao.MedicalRegistrationDao;
import com.example.hospital.api.db.pojo.DoctorPrescriptionEntity;
import com.example.hospital.api.exception.HospitalException;
import com.example.hospital.api.service.DoctorPrescriptionService;
import com.example.hospital.api.service.MisUserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorPrescriptionServiceImpl implements DoctorPrescriptionService {
    @Resource
    private MedicalRegistrationDao medicalRegistrationDao;

    @Resource
    private DoctorPrescriptionDao doctorPrescriptionDao;

    @Resource
    private MisUserService misUserService;

    @Override
    public PageUtils searchRegistrationByPage(Map param) {
        long count = medicalRegistrationDao.searchPrescriptionRegistrationCount(param);
        ArrayList list;
        if (count > 0) {
            list = medicalRegistrationDao.searchPrescriptionRegistrationByPage(param);
        }
        else {
            list = new ArrayList();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        return new PageUtils(list, count, page, length);
    }

    @Override
    public HashMap searchByRegistrationId(int registrationId, int userId, List<String> permissions) {
        HashMap baseInfo = medicalRegistrationDao.searchPrescriptionRegistrationBaseInfo(registrationId);
        if (baseInfo == null) {
            throw new HospitalException("挂号记录不存在");
        }
        this.checkPermission(baseInfo, userId, permissions);
        HashMap map = doctorPrescriptionDao.searchByRegistrationId(registrationId);
        if (map == null) {
            baseInfo.put("rpList", new JSONArray());
            return baseInfo;
        }
        baseInfo.putAll(map);
        String rp = MapUtil.getStr(baseInfo, "rp");
        JSONArray rpList;
        try {
            rpList = (rp != null && rp.trim().length() > 0) ? JSONUtil.parseArray(rp) : new JSONArray();
        }
        catch (Exception e) {
            throw new HospitalException("处方数据格式错误");
        }
        baseInfo.put("rpList", rpList);
        return baseInfo;
    }

    @Override
    public void save(Map param, int userId, List<String> permissions) {
        Integer registrationId = MapUtil.getInt(param, "registrationId");
        HashMap baseInfo = medicalRegistrationDao.searchPrescriptionRegistrationBaseInfo(registrationId);
        if (baseInfo == null) {
            throw new HospitalException("挂号记录不存在");
        }
        this.checkPermission(baseInfo, userId, permissions);
        if (!permissions.contains("ROOT") && !permissions.contains("PRESCRIPTION:INSERT") && !permissions.contains("PRESCRIPTION:UPDATE")) {
            Map userInfo = misUserService.searchUserInfoById(userId);
            String job = MapUtil.getStr(userInfo, "job");
            Integer refId = MapUtil.getInt(userInfo, "refId");
            if (!"医生".equals(job) || refId == null) {
                throw new HospitalException("No permission");
            }
        }
        Integer paymentStatus = MapUtil.getInt(baseInfo, "paymentStatus");
        if (paymentStatus == null || (paymentStatus != 1 && paymentStatus != 2)) {
            throw new HospitalException("当前挂号状态不允许开方");
        }

        String diagnosis = MapUtil.getStr(param, "diagnosis");
        String rp = MapUtil.getStr(param, "rp");
        JSONArray rpList;
        try {
            rpList = JSONUtil.parseArray(rp);
        }
        catch (Exception e) {
            throw new HospitalException("处方内容格式错误");
        }
        if (rpList == null || rpList.isEmpty()) {
            throw new HospitalException("处方内容不能为空");
        }

        HashMap current = doctorPrescriptionDao.searchByRegistrationId(registrationId);
        String uuid = current != null ? MapUtil.getStr(current, "uuid") : null;
        if (uuid == null || uuid.length() == 0) {
            uuid = IdUtil.simpleUUID().toUpperCase();
        }

        DoctorPrescriptionEntity entity = new DoctorPrescriptionEntity();
        entity.setUuid(uuid);
        entity.setPatientCardId(MapUtil.getInt(baseInfo, "patientCardId"));
        entity.setDiagnosis(diagnosis);
        entity.setSubDeptId(MapUtil.getInt(baseInfo, "subDeptId"));
        entity.setDoctorId(MapUtil.getInt(baseInfo, "doctorId"));
        entity.setRegistrationId(registrationId);
        entity.setRp(rpList.toString());
        doctorPrescriptionDao.upsert(entity);
    }

    private void checkPermission(HashMap baseInfo, int userId, List<String> permissions) {
        Map userInfo = misUserService.searchUserInfoById(userId);
        boolean isRoot = permissions.contains("ROOT");
        boolean canSelectAll = permissions.contains("PRESCRIPTION:SELECT");
        if (isRoot || canSelectAll) {
            return;
        }
        String job = MapUtil.getStr(userInfo, "job");
        Integer refId = MapUtil.getInt(userInfo, "refId");
        if (!"医生".equals(job) || refId == null) {
            throw new HospitalException("No permission");
        }
        Integer doctorId = MapUtil.getInt(baseInfo, "doctorId");
        if (!refId.equals(doctorId)) {
            throw new HospitalException("不能操作其他医生的挂号单");
        }
    }
}
