package com.exam408.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam408.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
