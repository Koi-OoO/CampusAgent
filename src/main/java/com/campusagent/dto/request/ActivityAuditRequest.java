package com.campusagent.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 活动审核请求，接收活动主键、审核结果及审核理由。
 *
 * <p>审核结果的合法取值及驳回理由必填规则由服务层统一校验。</p>
 */
@Data
public class ActivityAuditRequest {

    /**
     * 待审核活动的主键，不能为空。
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /**
     * 审核结果，不能为空；1 表示通过，2 表示驳回。
     */
    @NotNull(message = "审核结果不能为空")
    private Integer result;

    /**
     * 审核理由，最长 200 个字符；驳回时必填，通过时可为空。
     */
    @Size(max = 200, message = "审核理由长度不能超过 200 个字符")
    private String reason;
}
