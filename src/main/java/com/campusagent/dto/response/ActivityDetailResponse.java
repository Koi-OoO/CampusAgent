package com.campusagent.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端活动详情响应，包含活动全部公开资料及当前用户的报名状态。
 *
 * <p>发布人姓名由服务层关联查询后填充；currentUserSignupStatus 在未登录
 * 或用户未报名时为 null，其余取值为报名记录的状态编码。</p>
 */
@Data
public class ActivityDetailResponse {

    /**
     * 活动主键。
     */
    private Long id;

    /**
     * 活动标题。
     */
    private String title;

    /**
     * 活动详细描述。
     */
    private String description;

    /**
     * 活动封面图地址。
     */
    private String coverImage;

    /**
     * 活动地点。
     */
    private String location;

    /**
     * 活动开始时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 活动结束时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 报名截止时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime signupDeadline;

    /**
     * 活动状态编码：3 未开始，4 进行中，5 已结束。
     */
    private Integer status;

    /**
     * 活动分类主键，活动未设置分类时为 null。
     */
    private Integer categoryId;

    /**
     * 活动分类名称，分类已被删除或未设置时为 null。
     */
    private String categoryName;

    /**
     * 报名人数上限，0 表示不限人数。
     */
    private Integer maxParticipants;

    /**
     * 当前已报名人数。
     */
    private Integer currentParticipants;

    /**
     * 活动浏览次数。
     */
    private Integer viewCount;

    /**
     * 报名表单配置，JSON 字符串，可为 null。
     */
    private String signupFormConfig;

    /**
     * 发布人主键。
     */
    private Long publisherId;

    /**
     * 发布人姓名，优先取真实姓名，未填写时使用用户名。
     */
    private String publisherName;

    /**
     * 驳回理由，活动被驳回时填写，公开状态的活动该字段为 null。
     */
    private String rejectReason;

    /**
     * 取消理由，活动被取消时填写，公开状态的活动该字段为 null。
     */
    private String cancelReason;

    /**
     * 活动创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 当前用户对该活动的报名状态编码；未登录或未报名时为 null。
     * 0 待签到，1 已签到，2 已取消，3 已缺席。
     */
    private Integer currentUserSignupStatus;
}
