package com.campusagent.dto.response;

import com.campusagent.enums.UserRoleEnum;
import lombok.Data;

/**
 * 用户信息响应体。
 *
 * <p>只返回对外可见的用户资料，不包含密码等敏感字段。
 * role 序列化时输出 UserRoleEnum 枚举名（例如 "ADMIN"）。</p>
 */
@Data
public class UserInfoResponse {

    /**
     * 用户主键。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户真实姓名。
     */
    private String realName;

    /**
     * 学号。
     */
    private String studentId;

    /**
     * 用户所属学院。
     */
    private String college;

    /**
     * 联系电话。
     */
    private String phone;

    /**
     * 用户头像地址。
     */
    private String avatar;

    /**
     * 用户角色枚举，序列化为枚举名。
     */
    private UserRoleEnum role;
}
