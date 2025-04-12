package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.DoctorDao;
import com.example.hospital.patient.wx.api.db.dao.IllnessDao;
import com.example.hospital.patient.wx.api.service.DoctorService;
import com.example.hospital.patient.wx.api.service.IllnessService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class IllnessServiceImpl implements IllnessService {
    @Resource
    private IllnessDao illnessDao;



    @Override
    public PageUtils searchIllnessInfo(Map param) {
        ArrayList list =illnessDao.searchIllnessInfo(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }


}
