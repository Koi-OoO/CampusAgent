package com.campusagent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 超管调整用户角色的请求体，接收目标用户主键与目标角色编码。
 *
 * <p>基础非空校验由框架在参数绑定阶段执行；角色编码的合法取值
 * 交由服务层统一校验，避免校验规则散落在请求体中。</p>
 */
@Data
public class AdminRoleUpdateRequest {

    /**
     * 目标用户主键，不能为空。
     */
    @NotNull(message = "目标用户ID不能为空")
    private Long userId;

    /**
     * 目标角色编码，不能为空；1 表示管理员，2 表示超管。
     */
    @NotNull(message = "角色编码不能为空")
    private Integer role;
}
