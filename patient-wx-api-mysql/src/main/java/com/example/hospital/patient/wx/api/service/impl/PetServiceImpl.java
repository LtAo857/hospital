package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.PetDao;
import com.example.hospital.patient.wx.api.db.pojo.PetEntity;
import com.example.hospital.patient.wx.api.service.PetService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class PetServiceImpl implements PetService {
    @Resource
    private PetDao petDao;



    @Override
    public PageUtils searchByPage(Map param) {
        System.out.println(param);
        ArrayList list =petDao.searchByPage(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }

    @Override
    public PageUtils     searchByPageAndId(Map param) {
        ArrayList list =petDao.searchByPageAndId(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }


    @Override
    public void insert(Map param) {
        final PetEntity petEntity = BeanUtil.toBean(param, PetEntity.class);
        System.out.println(petEntity);
        petDao.insert(petEntity);
    }

    @Override
    public void update(Map param) {
        petDao.update(param);
    }

    @Override
    public HashMap searchById(int id) {
        return petDao.searchById(id);
    }

    @Override
    public void deleteByIds(Integer[] ids) {
        petDao.deleteByIds(ids);
    }


}
