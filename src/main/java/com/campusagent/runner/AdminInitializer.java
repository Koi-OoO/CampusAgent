package com.campusagent.runner;

import com.campusagent.entity.User;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 超管账号初始化启动任务，在应用启动时检查并创建初始超级管理员。
 *
 * <p>按用户名判断账号是否存在，已有同名账号时保留原有资料。
 * 新账号的密码由统一密码编码器编码后存储，日志中不输出明文密码。</p>
 */
@Slf4j
@Component
public class AdminInitializer implements CommandLineRunner {

    /**
     * 初始超管账号的用户名。
     */
    private static final String ADMIN_USERNAME = "superadmin";

    /**
     * 初始密码，仅用于交给密码编码器处理，不写入日志。
     */
    private static final String INITIAL_PASSWORD = "superadmin123";

    /**
     * 初始超管账号的真实姓名。
     */
    private static final String ADMIN_REAL_NAME = "超级管理员";

    /**
     * 账号启用状态，对应用户表中的正常账号状态。
     */
    private static final int ENABLED_STATUS = 1;

    /**
     * 用户服务，用于按用户名查询账号及保存新账号。
     */
    private final UserService userService;

    /**
     * 统一密码编码器，用于生成可供登录校验的密码哈希。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建超管账号初始化器，通过构造器注入所需依赖。
     *
     * @param userService 用户查询与持久化服务
     * @param passwordEncoder 统一密码编码器
     */
    public AdminInitializer(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 应用启动后检查超管账号，不存在时创建启用的超级管理员。
     *
     * <p>已有同名账号时直接跳过，保留其原有密码、角色和账号状态。</p>
     *
     * @param args 应用启动参数，本初始化任务不使用
     * @throws IllegalStateException 新账号保存失败时抛出，使初始化失败能够被明确感知
     */
    @Override
    public void run(String... args) {
        if (userService.getByUsername(ADMIN_USERNAME) != null) {
            log.info("超管账号已存在，跳过初始化");
            return;
        }

        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(INITIAL_PASSWORD));
        admin.setRealName(ADMIN_REAL_NAME);
        admin.setRole(UserRoleEnum.SUPER_ADMIN);
        admin.setStatus(ENABLED_STATUS);

        // 只有实际保存成功后才输出成功提示，避免持久化失败被误判为初始化完成。
        if (!userService.save(admin)) {
            throw new IllegalStateException("超管账号初始化失败");
        }
        log.info("超管账号初始化成功");
    }
}
