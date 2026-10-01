package com.campusagent.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名名单项响应对象。
 *
 * <p>该对象由报名记录和报名用户资料组装而成，报名表单数据保持 JSON 字符串原样返回。</p>
 */
@Data
public class SignupListResponse {

    /**
     * 报名记录主键。
     */
    private Long signupId;

    /**
     * 报名用户主键。
     */
    private Long userId;

    /**
     * 报名用户真实姓名；用户不存在或已逻辑删除时为 null。
     */
    private String realName;

    /**
     * 报名用户学号；用户不存在或已逻辑删除时为 null。
     */
    private String studentId;

    /**
     * 报名用户所属学院；用户不存在或已逻辑删除时为 null。
     */
    private String college;

    /**
     * 报名用户联系电话；用户不存在或已逻辑删除时为 null。
     */
    private String phone;

    /**
     * 报名表单数据，以 JSON 字符串原样保存和返回。
     */
    private String formData;

    /**
     * 报名状态编码：0 待签到，1 已签到，2 已取消，3 已缺席。
     */
    private Integer signupStatus;

    /**
     * 报名时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime signupTime;

    /**
     * 签到时间，未签到时为 null。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime checkinTime;
}
