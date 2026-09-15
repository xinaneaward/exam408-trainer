package com.exam408.service;

import com.exam408.dto.LoginRequest;
import com.exam408.dto.RegisterRequest;
import com.exam408.entity.User;

public interface UserService {
    User register(RegisterRequest request);
    User login(LoginRequest request);
    User getById(Long id);
}
