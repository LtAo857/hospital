package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.MessageDao;
import com.example.hospital.patient.wx.api.db.pojo.MessageEntity;
import com.example.hospital.patient.wx.api.service.MessageService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {
    @Resource
    private MessageDao messageDao;

    @Override
    public void sendMessage(int userId, byte type, String title, String content, Integer refId) {
        MessageEntity entity = new MessageEntity();
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setRefId(refId);
        messageDao.insert(entity);
    }

    @Override
    public PageUtils searchMessageByPage(Map param) {
        long count = messageDao.searchMessageCount(param);
        ArrayList list;
        if (count > 0) {
            list = messageDao.searchMessageByPage(param);
        } else {
            list = new ArrayList();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        return new PageUtils(list, count, page, length);
    }

    @Override
    public long searchUnreadCount(int userId) {
        return messageDao.searchUnreadCount(userId);
    }

    @Override
    public void readMessage(Map param) {
        messageDao.readMessage(param);
    }

    @Override
    public void readAllMessage(int userId) {
        messageDao.readAllMessage(userId);
    }
}
