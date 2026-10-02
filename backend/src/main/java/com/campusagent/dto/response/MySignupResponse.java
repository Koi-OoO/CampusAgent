package com.campusagent.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的报名列表项响应，包含报名记录及所报名活动的列表页信息。
 *
 * <p>活动标题、地点和时间由服务层批量关联查询后填充，
 * 报名状态使用数据库编码。</p>
 */
@Data
public class MySignupResponse {

    /**
     * 报名记录主键。
     */
    private Long signupId;

    /**
     * 活动主键。
     */
    private Long activityId;

    /**
     * 活动标题。
     */
    private String activityTitle;

    /**
     * 活动地点。
     */
    private String activityLocation;

    /**
     * 活动开始时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime activityStartTime;

    /**
     * 活动结束时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime activityEndTime;

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
