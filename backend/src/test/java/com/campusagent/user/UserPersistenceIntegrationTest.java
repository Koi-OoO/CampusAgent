package com.campusagent.user;

import com.campusagent.entity.User;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户数据访问集成测试，验证真实 MySQL 上的枚举映射、查询和逻辑删除。
 *
 * <p>先执行 sql/phase1_user.sql，再通过系统属性 campus.mysql.integration=true 启用。
 * 连接地址固定为本地 campus 库，每个测试方法完成后自动回滚事务。</p>
 */
@EnabledIfSystemProperty(named = "campus.mysql.integration", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/campus?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
        "logging.level.com.campusagent.mapper=INFO",
        // knife4j 自动配置与无 Web 环境的测试上下文冲突，测试中禁用。
        "knife4j.enable=false"
})
@ActiveProfiles("dev")
@Transactional
class UserPersistenceIntegrationTest {

    // 由 Spring 容器提供的真实用户服务，验证完整的数据访问调用链。
    @Autowired
    private UserService userService;

    // 直接读取数据库原始字段，用于确认枚举存储编码和逻辑删除标记。
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 验证各角色保存为指定整数编码，并通过两种 Service 查询正确还原完整用户资料。
     *
     * @param role 本次验证的用户角色
     */
    @ParameterizedTest
    @EnumSource(UserRoleEnum.class)
    void persistsRoleCodesAndQueriesProfiles(UserRoleEnum role) {
        User user = newTestUser();
        user.setRole(role);
        user.setRealName("集成测试用户");
        user.setStudentId("IT20260909");
        user.setCollege("测试学院");
        user.setPhone("13800000000");
        user.setAvatar("/avatars/phase1-test.png");
        user.setStatus(1);

        assertThat(userService.save(user)).isTrue();
        assertThat(user.getId()).isNotNull();
        Integer storedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM `user` WHERE id = ?", Integer.class, user.getId());
        assertThat(storedRole).isEqualTo(role.getCode());
        assertThat(UserRoleEnum.fromCode(storedRole)).isSameAs(role);

        User byUsername = userService.getByUsername(user.getUsername());
        assertThat(byUsername).isNotNull();
        assertThat(byUsername).usingRecursiveComparison()
                .ignoringFields("isDeleted", "createTime", "updateTime")
                .isEqualTo(user);
        assertThat(userService.getById(user.getId())).usingRecursiveComparison().isEqualTo(byUsername);
    }

    /**
     * 验证未显式填写角色、状态、删除标记和时间时，数据库默认值可以正常生效。
     */
    @Test
    void usesDatabaseDefaultsForNewUsers() {
        User user = newTestUser();

        assertThat(userService.save(user)).isTrue();
        User persisted = userService.getById(user.getId());

        assertThat(persisted).isNotNull();
        assertThat(persisted.getRole()).isEqualTo(UserRoleEnum.USER);
        assertThat(persisted.getStatus()).isEqualTo(1);
        assertThat(persisted.getIsDeleted()).isZero();
        assertThat(persisted.getCreateTime()).isNotNull();
        assertThat(persisted.getUpdateTime()).isNotNull();
    }

    /**
     * 验证更新已查询的实体时，创建时间保持不变，更新时间仍由数据库自动维护。
     */
    @Test
    void letsDatabaseMaintainTimestampsWhenUpdatingLoadedUsers() {
        User user = newTestUser();
        assertThat(userService.save(user)).isTrue();
        LocalDateTime originalTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        // 在回滚事务内设置较早的基准时间，避免依赖等待一秒等不稳定验证方式。
        jdbcTemplate.update("UPDATE `user` SET create_time = ?, update_time = ? WHERE id = ?",
                originalTime, originalTime, user.getId());
        User loaded = userService.getById(user.getId());
        loaded.setCollege("更新后的测试学院");
        loaded.setCreateTime(originalTime.minusYears(1));

        assertThat(userService.updateById(loaded)).isTrue();

        User updated = userService.getById(user.getId());
        assertThat(updated.getCollege()).isEqualTo("更新后的测试学院");
        assertThat(updated.getCreateTime()).isEqualTo(originalTime);
        assertThat(updated.getUpdateTime()).isAfter(originalTime);
    }

    /**
     * 验证逻辑删除保留数据库记录，并使用户名查询和 ID 查询均返回 null。
     */
    @Test
    void hidesLogicallyDeletedUsersFromBothQueries() {
        User user = newTestUser();
        assertThat(userService.getByUsername(user.getUsername())).isNull();
        assertThat(userService.save(user)).isTrue();

        assertThat(userService.removeById(user.getId())).isTrue();

        assertThat(userService.getByUsername(user.getUsername())).isNull();
        assertThat(userService.getById(user.getId())).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM `user` WHERE id = ?", Integer.class, user.getId())).isEqualTo(1);
    }

    /**
     * 创建仅填写必填字段的测试用户，随机用户名避免与本地已有数据冲突。
     *
     * @return 尚未保存的测试用户
     */
    private User newTestUser() {
        User user = new User();
        user.setUsername("phase1_it_" + UUID.randomUUID().toString().replace("-", ""));
        // 此处是事务回滚测试专用的占位值，密码加密由后续注册业务实现。
        user.setPassword("phase1-integration-placeholder");
        return user;
    }
}
