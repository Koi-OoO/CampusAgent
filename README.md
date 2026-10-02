# CampusAgent

校园综合服务平台，采用前后端同仓库管理。

## 目录

```text
CampusAgent/
├── backend/    # Spring Boot 后端
└── frontend/   # 前端应用
```

## 后端

后端项目位于 `backend/`，技术栈和接口说明见 [backend/README.md](backend/README.md)。

```bash
cd backend
mvn test
```

运行包含 MySQL 集成测试的完整测试套件：

```bash
cd backend
mvn test "-Dcampus.mysql.integration=true"
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

## 前端

前端代码放在 `frontend/`。前端项目初始化后，在该目录执行对应的依赖安装和开发命令。

```bash
cd frontend
npm install
npm run dev
```

## 开发约定

- 后端 Maven 命令在 `backend/` 目录执行。
- 前端 npm 命令在 `frontend/` 目录执行。
- 根目录用于统一管理前后端代码和 Git 仓库。
