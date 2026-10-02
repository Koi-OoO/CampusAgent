package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员取消活动的请求，接收活动主键及必须填写的取消理由。
 */
@Data
public class ActivityCancelRequest {

    /**
     * 待取消活动的主键，不能为空。
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /**
     * 活动取消理由，不能为空或仅包含空白字符，最长 200 个字符。
     */
    @NotBlank(message = "取消理由不能为空")
    @Size(max = 200, message = "取消理由长度不能超过 200 个字符")
    private String reason;
}
