package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 报名表单模板创建请求体，模板名称和表单配置均必填。
 */
@Data
public class TemplateCreateRequest {

    /**
     * 模板名称，不能为空或仅包含空白字符，最长 50 个字符。
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 50, message = "模板名称长度不能超过 50 个字符")
    private String name;

    /**
     * 表单配置，以 JSON 字符串传入，不能为空或仅包含空白字符。
     */
    @NotBlank(message = "模板配置不能为空")
    private String config;
}
