package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.DoctorDao;
import com.example.hospital.patient.wx.api.service.DoctorService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class DoctorServiceImpl implements DoctorService {
    @Resource
    private DoctorDao doctorDao;


    @Override
    public HashMap searchDoctorInfoById(int id) {
        HashMap map = doctorDao.searchDoctorInfoById(id);
        return map;
    }

    @Override
    public PageUtils searchDoctorInfo(Map param) {
        ArrayList list =doctorDao.searchDoctorInfo(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }
}
