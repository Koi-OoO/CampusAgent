# CampusAgent

校园综合服务平台后端，基础包名为 `com.campusagent`。

## 技术版本

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Spring Boot | 3.2.12 |
| MyBatis-Plus | 3.5.5 |
| MySQL Connector/J | 8.0.33 |
| jjwt | 0.12.6 |
| Hutool | 5.8.46 |

使用 Maven 构建。Spring Boot Starter、Lombok 及构建插件版本由 Spring Boot 3.2.12 管理。

`mysql:mysql-connector-java:8.0.33` 是官方迁移坐标，Maven 会自动解析为 `com.mysql:mysql-connector-j:8.0.33`。`pom.xml` 保留旧坐标，并使用 `mysql.version` 将版本锁定为 8.0.33。

## 建议目录结构

下列业务目录按开发进度添加，无需预先创建空文件。

```text
CampusAgent/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── campusagent/
    │   │           ├── CampusAgentApplication.java
    │   │           ├── common/
    │   │           │   ├── result/      # 错误码枚举、统一响应
    │   │           │   │   ├── ResultCode.java
    │   │           │   │   └── Result.java
    │   │           │   └── exception/   # 业务异常、全局异常处理
    │   │           │       ├── BusinessException.java
    │   │           │       └── GlobalExceptionHandler.java
    │   │           ├── config/          # MyBatis-Plus、Redis、JWT 配置
    │   │           ├── controller/      # HTTP 接口
    │   │           ├── service/         # 业务接口
    │   │           │   └── impl/        # 业务实现
    │   │           ├── mapper/          # MyBatis Mapper 接口
    │   │           ├── entity/          # 数据库实体
    │   │           ├── dto/             # 请求参数对象
    │   │           ├── vo/              # 响应视图对象
    │   │           ├── interceptor/     # 请求拦截器
    │   │           └── util/            # JWT 等工具
    │   └── resources/
    │       ├── application.yml
    │       └── mapper/                 # Mapper XML 文件
    └── test/
        └── java/
            └── com/
                └── campusagent/        # 测试代码
```

## 开发环境启动

使用 JDK 17，并确认 `mvn --version` 输出中的 Java 版本正确。

启动本地 MySQL 和 Redis：

- MySQL：`localhost:3306`，用户名 `root`，密码 `123456`。
- Redis：`localhost:6379`，无密码，数据库索引为 `0`。

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS campus
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

在项目根目录执行，默认使用 `dev`，HTTP 端口为 `8080`：

```bash
mvn spring-boot:run
```

## 生产环境启动

设置以下环境变量：

| 环境变量 | 内容 |
| --- | --- |
| `DB_URL` | 完整的 MySQL JDBC URL |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `REDIS_HOST` | Redis 主机地址 |
| `REDIS_PORT` | Redis 端口，可选，默认 `6379` |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | 独立随机密钥的 Base64 编码，解码后至少 32 字节 |

打包并启用 `prod`：

```bash
mvn clean package
java -jar target/campus-agent-1.0.0.jar --spring.profiles.active=prod
```

也可以设置 `SPRING_PROFILES_ACTIVE=prod` 切换环境。

## 配置约定

- `application.yml` 的三个 YAML 文档分别承载通用、dev 和 prod 配置。
- MyBatis-Plus 开启下划线转驼峰；dev 输出 Mapper SQL 日志，prod 使用 INFO 日志级别。
- 逻辑删除使用实体属性 `Integer deleted`，数据库对应列建议为 `TINYINT NOT NULL DEFAULT 0`；`0` 表示未删除，`1` 表示已删除。MyBatis-Plus 生成的 CRUD SQL 会应用逻辑删除规则，手写 SQL 需要自行处理。
- `jwt.expiration` 为 `604800000` 毫秒，即 7 天。
- `jwt` 是自定义配置，后续 JWT 工具需读取这些属性。签名密钥按 `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` 创建。
- dev 中的 JWT 密钥仅供本地开发，prod 从 `JWT_SECRET` 环境变量读取独立密钥。

## 统一响应与异常

- `Result<T>` 使用 `code`、`message`、`data` 三个字段；其中 `code` 是响应体中的业务码。
- 成功响应使用 `Result.success()`、`Result.success(data)` 或 `Result.success(message, data)`。
- 业务异常使用 `new BusinessException(ResultCode)` 或 `new BusinessException(ResultCode, message)`。
- 全局异常处理器保留业务异常的错误码和消息；参数校验失败返回 `1001`，未预期异常返回 `1000` 和“系统错误”，详细异常堆栈写入日志。
- 活动业务先使用通用占位码 `3000`，`3001–3099` 预留给后续具体业务错误。

## 用户中心数据层（Phase 1）

在项目根目录启动 MySQL 客户端，输入本地开发数据库密码：

```bash
mysql --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 -p
```

在客户端执行：

```sql
SOURCE sql/phase1_user.sql;
```

脚本在不存在时创建 `campus` 库和 `user` 表，使用 InnoDB 与 utf8mb4，不删除或重建已有表。已有表的结构变更需要另行编写迁移 SQL。

- `User` 映射全部用户字段，使用数据库自增主键；`UserMapper` 和 `UserService` 提供 MyBatis-Plus 的基础数据访问能力。
- `User.role` 使用 `UserRoleEnum`，通过 `@EnumValue` 按整数编码存储：`USER=0`、`ADMIN=1`、`SUPER_ADMIN=2`。`fromCode` 对 null 或未知编码返回 null。
- `status` 和 `isDeleted` 使用 `Integer`。`User` 在 `isDeleted` 上显式声明 `@TableLogic(value = "0", delval = "1")`，优先于全局 `deleted` 字段约定；普通查询自动过滤逻辑删除记录。
- `createTime` 和 `updateTime` 使用 `LocalDateTime`，由 MySQL 默认值和 `ON UPDATE` 维护。实体插入或更新不会回写这两个字段。
- `getByUsername(String)` 使用 Lambda 查询条件；`getById(Long)` 直接复用父类实现。查询不存在或已逻辑删除的用户时返回 null。
- 当前仅包含数据层和 Service 骨架，注册、密码加密、登录及 Controller 在后续阶段实现。

普通测试不要求启动 MySQL：

```bash
mvn test
```

创建表后，可显式启用真实 MySQL 集成测试：

```bash
mvn test "-Dcampus.mysql.integration=true"
```

集成测试使用 dev 配置中的数据库凭据，连接地址固定为 `127.0.0.1:3306/campus`。测试覆盖角色编码落库与还原、两种 Service 查询、数据库默认值、时间自动维护及逻辑删除；每个测试方法都在事务结束时回滚数据。
