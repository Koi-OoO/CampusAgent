package com.campusagent.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动修改请求，独立定义全部可编辑字段，活动主键由请求路径指定。
 *
 * <p>必填字段需要完整提交，可空字段传入 null 时清除对应旧值，时间及人数规则由服务层校验。</p>
 */
@Data
public class ActivityUpdateRequest {

    /**
     * 活动标题，不能为空或仅包含空白字符，最长 100 个字符。
     */
    @NotBlank(message = "活动标题不能为空")
    @Size(max = 100, message = "活动标题长度不能超过 100 个字符")
    private String title;

    /**
     * 活动描述，可以为空，最长 5000 个字符。
     */
    @Size(max = 5000, message = "活动描述长度不能超过 5000 个字符")
    private String description;

    /**
     * 活动封面图地址，可以为空，最长 255 个字符。
     */
    @Size(max = 255, message = "封面图地址长度不能超过 255 个字符")
    private String coverImage;

    /**
     * 活动分类主键，可以为空。
     */
    private Integer categoryId;

    /**
     * 活动地点，不能为空或仅包含空白字符，最长 200 个字符。
     */
    @NotBlank(message = "活动地点不能为空")
    @Size(max = 200, message = "活动地点长度不能超过 200 个字符")
    private String location;

    /**
     * 活动开始时间，格式为 yyyy-MM-dd HH:mm:ss。
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 活动结束时间，格式为 yyyy-MM-dd HH:mm:ss。
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 报名截止时间，格式为 yyyy-MM-dd HH:mm:ss。
     */
    @NotNull(message = "报名截止时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime signupDeadline;

    /**
     * 报名人数上限，必须提供且不能为负数，0 表示不限人数。
     */
    @NotNull(message = "人数上限不能为空")
    @Min(value = 0, message = "人数上限不能为负数")
    private Integer maxParticipants;

    /**
     * 报名表单配置，可以为空，以 JSON 字符串传入。
     */
    private String signupFormConfig;
}
