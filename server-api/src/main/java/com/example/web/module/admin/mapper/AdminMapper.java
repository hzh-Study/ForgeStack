package com.example.web.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.web.module.admin.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员数据访问接口。
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
