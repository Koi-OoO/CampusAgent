package com.campusagent.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campusagent.enums.SignupStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名记录实体，与 activity_signup 表对应，保存用户报名信息及签到状态。
 *
 * <p>报名状态通过枚举编码持久化，报名表单数据以 JSON 字符串保存；
 * 删除操作采用逻辑删除，报名时间、签到时间及创建更新时间统一由数据库维护。</p>
 */
@Data
@TableName("activity_signup")
public class ActivitySignup {

    /**
     * 报名记录主键，对应 BIGINT，由数据库自动递增生成。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 被报名活动的主键，对应 BIGINT。
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 报名用户的主键，对应 BIGINT。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 报名表单数据，以 JSON 字符串保存，可为空。
     */
    @TableField("form_data")
    private String formData;

    /**
     * 报名状态，通过 SignupStatusEnum 的 code 字段持久化，数据库默认值为待签到。
     */
    private SignupStatusEnum status;

    /**
     * 报名时间，由数据库默认值生成，不参与应用层插入和更新。
     */
    @TableField(value = "signup_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime signupTime;

    /**
     * 签到时间，签到前为 null。
     */
    @TableField("checkin_time")
    private LocalDateTime checkinTime;

    /**
     * 逻辑删除标记：0 表示未删除，1 表示已删除，数据库默认值为 0。
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /**
     * 创建时间，由数据库默认值生成，不参与应用层插入和更新。
     */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /**
     * 更新时间，由数据库自动维护，不回写应用层中的时间值。
     */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
