package com.example.hospital.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.dao.FavoriteDao;
import com.example.hospital.api.service.FavoriteService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    @Resource
    private FavoriteDao favoriteDao;

    @Override
    public PageUtils searchByPage(Map param) {
        long count = favoriteDao.searchCount(param);
        ArrayList list;
        if (count > 0) {
            list = favoriteDao.searchByPage(param);
        } else {
            list = new ArrayList();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        return new PageUtils(list, count, page, length);
    }
}
