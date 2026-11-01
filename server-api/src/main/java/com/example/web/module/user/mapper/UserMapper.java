package com.example.web.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.web.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 前台用户数据访问接口。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
