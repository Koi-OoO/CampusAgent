package com.campusagent.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名表单模板实体，与 signup_form_template 表对应。
 *
 * <p>模板采用逻辑删除，创建时间和更新时间统一由数据库维护。</p>
 */
@Data
@TableName("signup_form_template")
public class SignupFormTemplate {

    /**
     * 模板主键，对应 INT，由数据库自动递增生成。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 模板名称，最长 50 个字符。
     */
    private String name;

    /**
     * 表单配置，以 JSON 字符串存储，描述模板包含的字段及校验规则。
     */
    private String config;

    /**
     * 逻辑删除标记：0 表示未删除，1 表示已删除。
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /**
     * 创建时间，由数据库默认值生成，不参与应用层插入和更新。
     */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /**
     * 更新时间，由数据库自动维护，不回写应用层中的时间值。
     */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
