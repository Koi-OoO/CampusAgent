package com.campusagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusagent.entity.ActivitySignup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动报名记录数据访问接口，继承 MyBatis-Plus 提供的基础增删改查能力。
 *
 * <p>逻辑删除、状态枚举持久化及数据库字段映射由报名记录实体和枚举上的注解控制。</p>
 */
@Mapper
public interface ActivitySignupMapper extends BaseMapper<ActivitySignup> {
}
