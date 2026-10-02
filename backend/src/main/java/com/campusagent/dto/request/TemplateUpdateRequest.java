package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 报名表单模板修改请求体，模板主键、名称和表单配置均必填。
 */
@Data
public class TemplateUpdateRequest {

    /**
     * 模板主键，用于标记待修改的目标模板。
     */
    @NotNull(message = "模板编号不能为空")
    private Integer id;

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
