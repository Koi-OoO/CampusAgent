package com.campusagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.entity.User;

/**
 * 用户服务接口，在 MyBatis-Plus 基础服务能力上提供用户查询入口。
 *
 * <p>当前阶段仅提供数据访问骨架，后续在此扩展注册、登录和资料维护等业务。</p>
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询未被逻辑删除的用户。
     *
     * @param username 待查询的用户名
     * @return 匹配的用户，不存在时返回 null
     */
    User getByUsername(String username);

    /**
     * 根据用户主键查询未被逻辑删除的用户。
     *
     * @param id 用户主键，对应数据库 BIGINT 类型
     * @return 匹配的用户，不存在时返回 null
     */
    User getById(Long id);
}
