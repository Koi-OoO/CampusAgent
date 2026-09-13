package com.campusagent.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campusagent.enums.ActivityStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动实体，与 activity 表对应，保存活动资料、报名配置及生命周期状态。
 *
 * <p>活动状态通过枚举编码持久化，报名表单配置暂以 JSON 字符串保存。
 * 删除操作采用逻辑删除，创建时间和更新时间统一由数据库维护。</p>
 */
@Data
@TableName("activity")
public class Activity {

    /**
     * 活动主键，对应 BIGINT，由数据库自动递增生成。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动标题，最长 100 个字符。
     */
    private String title;

    /**
     * 活动描述，对应 TEXT，可为空。
     */
    private String description;

    /**
     * 活动封面图地址，最长 255 个字符。
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 活动分类主键，对应 activity_category 表中的 INT 类型主键。
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 活动地点，最长 200 个字符。
     */
    private String location;

    /**
     * 活动开始时间。
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 活动结束时间。
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 报名截止时间。
     */
    @TableField("signup_deadline")
    private LocalDateTime signupDeadline;

    /**
     * 报名人数上限，数据库默认值为 50，0 表示不限人数。
     */
    @TableField("max_participants")
    private Integer maxParticipants;

    /**
     * 当前报名人数，数据库默认值为 0。
     */
    @TableField("current_participants")
    private Integer currentParticipants;

    /**
     * 活动浏览量，数据库默认值为 0。
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 报名表单配置，对应 JSON 字段，暂以合法的 JSON 字符串保存。
     */
    @TableField("signup_form_config")
    private String signupFormConfig;

    /**
     * 活动状态，通过 ActivityStatusEnum 的 code 字段持久化，数据库默认值为草稿。
     */
    private ActivityStatusEnum status;

    /**
     * 审核驳回理由，最长 200 个字符。
     */
    @TableField("reject_reason")
    private String rejectReason;

    /**
     * 活动取消理由，最长 200 个字符。
     */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 活动发布人的用户主键，对应 BIGINT。
     */
    @TableField("publisher_id")
    private Long publisherId;

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
