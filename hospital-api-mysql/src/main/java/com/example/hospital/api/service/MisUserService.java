package com.example.hospital.api.service;

import java.util.Map;

public interface MisUserService {
    public Integer login(Map param);

    public Map searchUserInfoById(int id);
}
