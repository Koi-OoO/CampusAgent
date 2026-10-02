package com.campusagent.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端活动列表项响应，仅包含列表页展示所需的字段。
 *
 * <p>不包含活动描述等长文本，减少列表接口的响应体积；
 * 状态使用数据库编码，分类名称由服务层关联查询后填充。</p>
 */
@Data
public class ActivityListResponse {

    /**
     * 活动主键。
     */
    private Long id;

    /**
     * 活动标题。
     */
    private String title;

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
}
