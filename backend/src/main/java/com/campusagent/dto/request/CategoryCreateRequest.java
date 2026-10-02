package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 活动分类创建请求体，分类名称必填，排序值可以省略。
 */
@Data
public class CategoryCreateRequest {

    /**
     * 分类名称，不能为空或仅包含空白字符，最长 50 个字符。
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过 50 个字符")
    private String name;

    /**
     * 排序值，数字越小越靠前；省略时默认使用 0，显式传入 null 时也按 0 处理。
     */
    private Integer sort = 0;
}
