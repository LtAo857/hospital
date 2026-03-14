package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class MessageEntity {
    private Integer id;
    private Integer userId;
    private Byte type;
    private String title;
    private String content;
    private Integer refId;
    private Boolean isRead;
    private Date createTime;
}
