package com.tulip.service;

import com.tulip.entity.User;

public interface UserService {
    User register(String email, String rawPassword, String fullName, String phone);
}
