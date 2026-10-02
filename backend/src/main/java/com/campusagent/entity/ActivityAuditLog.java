package com.campusagent.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动审核记录实体，保存每次审核通过或驳回的结果及审核人信息。
 *
 * <p>审核记录不使用逻辑删除，也不维护更新时间，创建时间由数据库生成。</p>
 */
@Data
@TableName("activity_audit_log")
public class ActivityAuditLog {

    /**
     * 审核记录主键，由数据库自动递增生成。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 被审核活动的主键。
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 执行审核的管理员主键。
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 审核结果：1 表示通过，2 表示驳回。
     */
    private Integer result;

    /**
     * 审核理由，最长 200 个字符；驳回时必填，通过时可为空。
     */
    private String reason;

    /**
     * 审核记录创建时间，由数据库维护，不参与应用层插入和更新。
     */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
