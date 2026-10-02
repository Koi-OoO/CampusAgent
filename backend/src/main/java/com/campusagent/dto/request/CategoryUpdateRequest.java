package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 活动分类修改请求体，分类主键和名称必填，排序值允许为空。
 */
@Data
public class CategoryUpdateRequest {

    /**
     * 分类主键，必须与请求路径中的分类主键一致。
     */
    @NotNull(message = "分类编号不能为空")
    private Integer id;

    /**
     * 分类名称，不能为空或仅包含空白字符，最长 50 个字符。
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过 50 个字符")
    private String name;

    /**
     * 排序值，省略或传入 null 时保留原有排序值。
     */
    private Integer sort;
}
