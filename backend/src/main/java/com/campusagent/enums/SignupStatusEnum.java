package com.campusagent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 报名状态枚举，统一定义活动报名记录的数据库编码与中文描述。
 *
 * <p>通过 MyBatis-Plus 的 EnumValue 注解按 code 字段持久化，
 * 与活动状态枚举保持相同的持久化约定。</p>
 */
@Getter
public enum SignupStatusEnum {

    /**
     * 待签到，报名成功后的初始状态。
     */
    PENDING(0, "待签到"),

    /**
     * 已签到，活动现场完成签到。
     */
    CHECKED_IN(1, "已签到"),

    /**
     * 已取消，用户主动取消报名。
     */
    CANCELLED(2, "已取消"),

    /**
     * 已缺席，活动结束后未签到。
     */
    ABSENT(3, "已缺席");

    /**
     * 数据库状态编码，对应 activity_signup 表中的 TINYINT 类型 status 字段。
     */
    @EnumValue
    private final Integer code;

    /**
     * 报名状态的中文描述。
     */
    private final String description;

    /**
     * 创建报名状态枚举值。
     *
     * @param code 数据库中的报名状态编码
     * @param description 报名状态的中文描述
     */
    SignupStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据数据库状态编码获取对应的报名状态。
     *
     * @param code 报名状态编码，允许为 null
     * @return 匹配的报名状态；编码为 null 或未定义时返回 null
     */
    public static SignupStatusEnum fromCode(Integer code) {
        for (SignupStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
