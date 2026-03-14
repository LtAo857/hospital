package com.example.hospital.patient.wx.api.service;

import com.example.hospital.patient.wx.api.common.PageUtils;

import java.util.Map;

public interface MessageService {
    public void sendMessage(int userId, byte type, String title, String content, Integer refId);
    public PageUtils searchMessageByPage(Map param);
    public long searchUnreadCount(int userId);
    public void readMessage(Map param);
    public void readAllMessage(int userId);
}
