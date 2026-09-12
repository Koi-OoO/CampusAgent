package com.campusagent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户角色枚举，统一定义数据库角色编码与中文描述。
 *
 * <p>通过 MyBatis-Plus 的 EnumValue 注解按 code 字段持久化，
 * 避免使用枚举名称或声明顺序作为数据库角色值。</p>
 */
@Getter
public enum UserRoleEnum {

    /**
     * 普通用户。
     */
    USER(0, "普通用户"),

    /**
     * 管理员。
     */
    ADMIN(1, "管理员"),

    /**
     * 超级管理员。
     */
    SUPER_ADMIN(2, "超管");

    /**
     * 数据库角色编码，对应 user 表中的 TINYINT 类型 role 字段。
     */
    @EnumValue
    private final Integer code;

    /**
     * 角色的中文描述。
     */
    private final String description;

    /**
     * 创建角色枚举值。
     *
     * @param code 数据库中的角色编码
     * @param description 角色的中文描述
     */
    UserRoleEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据数据库角色编码获取对应枚举。
     *
     * @param code 角色编码，允许为 null
     * @return 匹配的角色枚举；编码为 null 或未定义时返回 null
     */
    public static UserRoleEnum fromCode(Integer code) {
        // 使用非空枚举编码进行比较，使 null 输入也能安全返回未匹配结果。
        for (UserRoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        // 未知编码不自动降级为任何角色，交由调用方决定后续处理方式。
        return null;
    }
}
