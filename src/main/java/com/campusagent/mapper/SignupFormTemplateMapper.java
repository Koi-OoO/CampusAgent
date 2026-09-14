package com.campusagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusagent.entity.SignupFormTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报名表单模板数据访问接口，继承 MyBatis-Plus 提供的基础增删改查能力。
 *
 * <p>逻辑删除和数据库字段映射由报名表单模板实体上的注解统一控制。</p>
 */
@Mapper
public interface SignupFormTemplateMapper extends BaseMapper<SignupFormTemplate> {
}
