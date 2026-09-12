package com.campusagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.entity.User;
import com.campusagent.mapper.UserMapper;
import com.campusagent.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类，复用 MyBatis-Plus 的基础数据访问能力。
 *
 * <p>查询自动遵循 User 实体的逻辑删除规则，当前不包含登录校验或密码处理逻辑。</p>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

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
}
