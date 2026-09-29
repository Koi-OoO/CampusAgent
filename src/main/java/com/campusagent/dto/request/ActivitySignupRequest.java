package com.campusagent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 活动报名请求体，活动主键必填，报名表单数据可选。
 */
@Data
public class ActivitySignupRequest {

    /**
     * 待报名的活动主键，不能为空。
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /**
     * 报名表单数据，JSON 字符串，活动未配置报名表单时可为空。
     */
    private String formData;
}
