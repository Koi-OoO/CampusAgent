package com.campusagent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 活动状态枚举，统一定义活动生命周期中的数据库编码与中文描述。
 *
 * <p>通过 MyBatis-Plus 的 EnumValue 注解按 code 字段持久化，
 * 避免使用枚举名称或声明顺序作为数据库状态值。</p>
 */
@Getter
public enum ActivityStatusEnum {

    /**
     * 草稿，尚未提交审核。
     */
    DRAFT(0, "草稿"),

    /**
     * 待审核，等待管理员审核。
     */
    PENDING_AUDIT(1, "待审核"),

    /**
     * 已驳回，审核未通过。
     */
    REJECTED(2, "已驳回"),

    /**
     * 未开始，审核通过且活动尚未开始。
     */
    NOT_STARTED(3, "未开始"),

    /**
     * 进行中，活动已开始且尚未结束。
     */
    IN_PROGRESS(4, "进行中"),

    /**
     * 已结束，活动已经结束。
     */
    ENDED(5, "已结束"),

    /**
     * 已取消，活动已被取消。
     */
    CANCELLED(6, "已取消");

    /**
     * 数据库状态编码，对应 activity 表中的 TINYINT 类型 status 字段。
     */
    @EnumValue
    private final Integer code;

    /**
     * 活动状态的中文描述。
     */
    private final String description;

    /**
     * 创建活动状态枚举值。
     *
     * @param code 数据库中的活动状态编码
     * @param description 活动状态的中文描述
     */
    ActivityStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据数据库状态编码获取对应的活动状态。
     *
     * @param code 活动状态编码，允许为 null
     * @return 匹配的活动状态；编码为 null 或未定义时返回 null
     */
    public static ActivityStatusEnum fromCode(Integer code) {
        for (ActivityStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
