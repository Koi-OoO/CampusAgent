package com.campusagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusagent.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动数据访问接口，继承 MyBatis-Plus 提供的基础增删改查能力。
 *
 * <p>逻辑删除、状态枚举持久化及数据库字段映射由活动实体和枚举上的注解控制。</p>
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}
