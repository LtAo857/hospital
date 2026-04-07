package com.example.hospital.patient.wx.api.agent.tool;

import com.example.hospital.patient.wx.api.service.UserInfoCardService;
import com.example.hospital.patient.wx.api.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;

@Component
public class UserAgentTools {
    @Resource
    private UserService userService;

    @Resource
    private UserInfoCardService userInfoCardService;

    public HashMap getCurrentUser(Integer userId) {
        if (userId == null) {
            return null;
        }
        return userService.searchUserInfo(userId);
    }

    public boolean hasUserCard(Integer userId) {
        return userId != null && userInfoCardService.hasUserInfoCard(userId);
    }

    public HashMap getUserCardDetail(Integer userId) {
        if (userId == null) {
            return null;
        }
        return userInfoCardService.searchUserInfoCard(userId);
    }
}
