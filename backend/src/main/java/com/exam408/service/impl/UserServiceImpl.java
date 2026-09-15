package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam408.dto.LoginRequest;
import com.exam408.dto.RegisterRequest;
import com.exam408.entity.User;
import com.exam408.mapper.UserMapper;
import com.exam408.service.UserService;
import com.exam408.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.hash(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        userMapper.insert(user);
        return user;
    }

    @Override
    public User login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        boolean matched;
        if (PasswordUtil.isPbkdf2(user.getPassword())) {
            matched = PasswordUtil.matches(request.getPassword(), user.getPassword());
        } else if (PasswordUtil.isLegacyMd5(user.getPassword())) {
            // 兼容历史无盐 MD5：校验通过后自动热升级为 PBKDF2
            String legacyMd5 = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
            matched = legacyMd5.equals(user.getPassword());
            if (matched) {
                String upgraded = PasswordUtil.hash(request.getPassword());
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .set(User::getPassword, upgraded));
                user.setPassword(upgraded);
            }
        } else {
            matched = false;
        }
        if (!matched) {
            throw new RuntimeException("用户名或密码错误");
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}
