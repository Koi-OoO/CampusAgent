package com.campusagent.dto.response;

import lombok.Data;

/**
 * 活动报名统计响应对象，汇总活动容量、报名数量、签到数量及关键转化比例。
 *
 * <p>报名率按当前有效报名人数除以报名上限计算；当活动报名上限为 0 时表示不限人数，
 * 此时报名率没有明确分母，返回 null。签到率按已签到人数除以报名总记录数计算；
 * 当报名总记录数为 0 时返回 null。</p>
 */
@Data
public class ActivityStatisticsResponse {

    /**
     * 活动主键。
     */
    private Long activityId;

    /**
     * 活动标题。
     */
    private String activityTitle;

    /**
     * 报名人数上限，0 表示不限人数。
     */
    private Integer maxParticipants;

    /**
     * 当前有效报名人数，通常不包含已取消报名记录。
     */
    private Integer currentParticipants;

    /**
     * 报名记录总数，包含待签到、已签到、已缺席和已取消等未被逻辑删除的记录。
     */
    private Long totalSignups;

    /**
     * 已签到报名记录数量。
     */
    private Long checkedIn;

    /**
     * 已缺席报名记录数量。
     */
    private Long absent;

    /**
     * 已取消报名记录数量。
     */
    private Long cancelled;

    /**
     * 报名率，等于 currentParticipants / maxParticipants；不限人数时为 null。
     */
    private Double signupRate;

    /**
     * 签到率，等于 checkedIn / totalSignups；报名记录总数为 0 时为 null。
     */
    private Double checkinRate;
}
