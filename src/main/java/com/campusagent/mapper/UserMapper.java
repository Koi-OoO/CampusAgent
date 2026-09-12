package com.campusagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusagent.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口，继承 MyBatis-Plus 提供的基础增删改查能力。
 *
 * <p>用户角色映射和逻辑删除规则由 User 实体中的注解统一控制，
 * 当前阶段无需编写额外的 Mapper XML。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
