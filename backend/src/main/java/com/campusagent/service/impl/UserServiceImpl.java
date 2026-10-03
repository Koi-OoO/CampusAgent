package com.campusagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.entity.User;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.mapper.UserMapper;
import com.campusagent.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类，复用 MyBatis-Plus 的基础数据访问能力。
 *
 * <p>查询自动遵循 User 实体的逻辑删除规则；角色调整仅允许超管执行，
 * 调整范围限定为管理员与超管，防止越权授予或收回角色。</p>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 管理员角色的数据库编码。 */
    private static final int ROLE_ADMIN = 1;

    /** 超管角色的数据库编码。 */
    private static final int ROLE_SUPER_ADMIN = 2;

    /**
     * 根据用户名查询用户，使用 Lambda 表达式引用实体字段。
     *
     * @param username 待查询的用户名
     * @return 未被逻辑删除的用户，不存在时返回 null
     */
    @Override
    public User getByUsername(String username) {
        // 使用参数绑定构造用户名查询条件，避免手工拼接 SQL。
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username);
        // username 具有唯一约束，可直接复用基础服务的单条查询方法。
        return getOne(queryWrapper);
    }

    /**
     * 根据学号查询用户，供注册时执行学号唯一性校验。
     *
     * @param studentId 待查询的学号
     * @return 未被逻辑删除的用户，不存在时返回 null
     */
    @Override
    public User getByStudentId(String studentId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStudentId, studentId);
        return getOne(queryWrapper);
    }

    /**
     * 根据用户主键查询用户，直接委托父类实现。
     *
     * @param id 用户主键
     * @return 未被逻辑删除的用户，不存在时返回 null
     */
    @Override
    public User getById(Long id) {
        // 调用父类已有的按主键查询能力，保留 MyBatis-Plus 的逻辑删除过滤。
        return super.getById(id);
    }

    /**
     * 超管调整目标用户的角色，目标角色仅允许为管理员或超管。
     *
     * <p>校验顺序为：操作人必须是超管 → 目标用户必须存在 → 角色编码必须合法 →
     * 不允许修改自己的角色。更新通过 LambdaUpdateWrapper 按主键定位记录，
     * 只写 role 一个字段，不影响用户其他资料。</p>
     *
     * @param userId 目标用户主键
     * @param role 目标角色编码，1 表示管理员，2 表示超管
     * @param operatorId 当前操作人（超管）主键
     * @throws BusinessException 操作人不存在或不是超管、目标用户不存在、角色编码不合法、试图修改自己的角色时抛出
     */
    @Override
    public void updateUserRole(Long userId, Integer role, Long operatorId) {
        // 操作人主键来自已校验的 Token，但角色可能在签发后发生变化，更新前再次从数据库确认。
        User operator = getById(operatorId);
        if (operator == null || operator.getRole() != UserRoleEnum.SUPER_ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 目标用户必须存在且未被逻辑删除。
        if (getById(userId) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 角色编码只允许管理员和超管，避免将用户调整为普通用户或写入非法值。
        if (role == null || (role != ROLE_ADMIN && role != ROLE_SUPER_ADMIN)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色值不合法");
        }

        // 修改自己的角色会使当前超管账号失去权限，直接拒绝。
        if (operatorId.equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能修改自己的角色");
        }

        // 只更新角色字段，按主键定位并带角色条件写入，防止并发修改被无条件覆盖。
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getRole, role);
        if (!update(updateWrapper)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 分页查询全部未被逻辑删除的用户，按创建时间倒序返回。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 用户分页结果，包含记录及总数
     * @throws BusinessException 页码或每页条数为空或不大于零时抛出
     */
    @Override
    public IPage<User> listUsers(Integer page, Integer size) {
        // 分页参数非法时提前终止，避免向分页插件传入无效页码。
        if (page == null || page < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "页码必须大于 0");
        }
        if (size == null || size < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每页条数必须大于 0");
        }

        // 按创建时间倒序并追加主键倒序，保证同秒创建的用户分页顺序稳定。
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime)
                .orderByDesc(User::getId);
        return page(new Page<User>(page, size), queryWrapper);
    }
}
