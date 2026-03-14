package com.example.hospital.patient.wx.api.db.dao;

import com.example.hospital.patient.wx.api.db.pojo.MessageEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MessageDao {
    public int insert(MessageEntity entity);
    public long searchMessageCount(Map param);
    public ArrayList<HashMap> searchMessageByPage(Map param);
    public long searchUnreadCount(int userId);
    public int readMessage(Map param);
    public int readAllMessage(int userId);
}
