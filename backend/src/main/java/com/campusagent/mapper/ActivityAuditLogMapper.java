package com.campusagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusagent.entity.ActivityAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动审核记录数据访问接口，提供审核记录的基础持久化能力。
 *
 * <p>审核记录实体不配置逻辑删除，审核业务通过事务保证状态更新和记录插入同时生效。</p>
 */
@Mapper
public interface ActivityAuditLogMapper extends BaseMapper<ActivityAuditLog> {
}
