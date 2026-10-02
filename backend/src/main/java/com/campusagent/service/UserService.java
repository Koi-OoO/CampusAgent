package com.campusagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.entity.User;

/**
 * 用户服务接口，在 MyBatis-Plus 基础服务能力上提供用户查询、角色调整等业务入口。
 *
 * <p>除基础数据访问骨架外，本接口还承担超管对用户角色的管理能力，
 * 后续可继续在此扩展资料维护等业务。</p>
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

    /**
     * 超管调整目标用户的角色，操作人和目标用户的合法性均在服务层校验。
     *
     * @param userId 目标用户主键
     * @param role 目标角色编码，1 表示管理员，2 表示超管
     * @param operatorId 当前操作人（超管）主键
     * @throws com.campusagent.common.exception.BusinessException 操作人非超管、目标用户不存在或角色编码不合法时抛出
     */
    void updateUserRole(Long userId, Integer role, Long operatorId);

    /**
     * 分页查询全部未被逻辑删除的用户，按创建时间倒序返回。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 用户分页结果，包含记录及总数
     */
    IPage<User> listUsers(Integer page, Integer size);
}
