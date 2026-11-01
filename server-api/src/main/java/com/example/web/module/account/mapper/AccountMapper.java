package com.example.web.module.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.web.module.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一账号数据访问接口。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
