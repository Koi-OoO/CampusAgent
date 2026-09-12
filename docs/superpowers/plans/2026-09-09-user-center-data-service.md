# 用户中心数据层与 Service 骨架实现计划

**目标：** 在本地 MySQL 的 campus 库创建 user 表，新增用户实体、角色枚举、Mapper 和 Service，不新增 Controller。

**实现约定：** 以用户提供的字段、默认值和索引为准。角色在 Java 中使用枚举，在数据库中使用整数编码。其他 TINYINT 字段使用 Integer，DATETIME 使用 LocalDateTime。逻辑删除字段为 is_deleted，注解显式覆盖项目全局 deleted 字段约定。时间字段由数据库默认值及 ON UPDATE 维护。

**技术栈：** Java 17、Spring Boot 3.2.12、MyBatis-Plus 3.5.5、Lombok、MySQL 8。

## 实施步骤

- [x] 检查项目配置和 MySQL 元数据：本地实例可连接，campus 库不存在。
- [x] 新增 `sql/phase1_user.sql`：创建 campus 库并执行用户指定的建表结构，采用 InnoDB 和 utf8mb4；不删除或重建已有表。
- [x] 新增 `src/main/java/com/campusagent/enums/UserRoleEnum.java`：定义 USER、ADMIN、SUPER_ADMIN，使用 `@EnumValue` 标记 Integer 编码，使用 Lombok `@Getter`，为构造器和 `fromCode` 编写中文 JavaDoc。空值或未知编码返回 null。
- [x] 新增 `src/main/java/com/campusagent/entity/User.java`：使用 `@Data`、`@TableName(autoResultMap = true)`、自增 `@TableId` 和显式 `@TableLogic(value = "0", delval = "1")`，映射全部 13 个字段。
- [x] 新增 `src/main/java/com/campusagent/mapper/UserMapper.java`：使用 `@Mapper` 并继承 `BaseMapper<User>`。
- [x] 新增 `src/main/java/com/campusagent/service/UserService.java` 和 `service/impl/UserServiceImpl.java`：继承 MyBatis-Plus 基础类型，通过 LambdaQueryWrapper 查询用户名，通过父类实现查询 ID。所有类和方法使用中文 JavaDoc。
- [x] 执行 SQL，核对实际列定义、默认值、唯一索引和表引擎。
- [x] 执行现有测试与真实 MySQL 集成验证，检查枚举映射、数据库默认值、两种查询和逻辑删除。
- [x] 复核变更并更新 README，记录脚本与验证命令。

## 验证方式

- 新增 `src/test/java/com/campusagent/user/UserPersistenceIntegrationTest.java`，通过 Spring 容器和真实 Mapper 调用 Service，不使用 Mapper mock。
- 测试显式连接 `127.0.0.1:3306/campus`，使用 dev 配置中的凭据；仅在 `campus.mysql.integration=true` 时启用。
- 通过事务回滚撤销每次测试中的插入和逻辑删除；测试用户名带随机后缀。
- `mvn test`：默认执行不依赖 MySQL 的现有测试，跳过需要显式启用的 MySQL 测试。
- `mvn -Dcampus.mysql.integration=true test`：执行现有测试及本次 MySQL 集成测试，预期全部通过。
- 实现完成后只读检查 user 表剩余数据，确认没有遗留测试记录。

## 范围

本次只提供数据访问能力和 Service 骨架。注册、密码加密、登录、JWT 解析和 Controller 由后续阶段实现。工作目录未初始化 Git，本次不创建提交。

## 验证结果

- 已执行 `sql/phase1_user.sql`，创建本地 `campus.user`；实际表结构为 13 列、InnoDB，主键、用户名唯一约束、默认值和中文字段注释均已核对。
- `mvn test "-Dcampus.mysql.integration=true"`：14 项测试通过，包含 8 项既有测试和 6 项真实 MySQL 集成测试，0 失败、0 错误、0 跳过。
- `mvn test`：8 项既有测试通过，MySQL 测试按预期跳过，默认测试不启动数据库集成上下文。
- 数据库复核：`SELECT COUNT(*) FROM campus.user` 返回 0，未遗留测试记录。
- 独立代码审查未发现阻塞问题；新增 Java 文件均具备中文 JavaDoc，未新增 Controller。
