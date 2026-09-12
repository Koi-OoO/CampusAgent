package com.campusagent.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campusagent.enums.UserRoleEnum;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户实体，与 campus 数据库中的 user 表对应。
 *
 * <p>角色通过枚举编码存储，删除操作采用逻辑删除。
 * Lombok 自动生成属性访问方法，创建时间和更新时间由数据库维护。</p>
 */
@Data
@TableName(value = "user", autoResultMap = true)
public class User {

    /**
     * 用户主键，对应 BIGINT，由数据库自动递增生成。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，在数据库中具有唯一约束。
     */
    private String username;

    /**
     * 密码存储值，后续注册业务应写入密码哈希，避免在字符串日志中输出此字段。
     */
    @ToString.Exclude
    private String password;

    /**
     * 用户真实姓名。
     */
    @TableField("real_name")
    private String realName;

    /**
     * 学号，使用字符串保留可能存在的前导零。
     */
    @TableField("student_id")
    private String studentId;

    /**
     * 用户所属学院。
     */
    private String college;

    /**
     * 联系电话，使用字符串保存号码及其格式。
     */
    private String phone;

    /**
     * 用户头像地址。
     */
    private String avatar;

    /**
     * 用户角色，数据库中以 Integer 编码保存：0 为普通用户，1 为管理员，2 为超管。
     */
    private UserRoleEnum role;

    /**
     * 账号状态，对应 TINYINT：0 表示禁用，1 表示正常。
     */
    private Integer status;

    /**
     * 逻辑删除标记，对应 TINYINT：0 表示未删除，1 表示已删除。
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
     * 更新时间，由数据库生成和自动更新，避免回写查询得到的旧时间。
     */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
