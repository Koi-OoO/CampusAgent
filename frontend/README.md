# CampusAgent Frontend

校园综合服务平台前端，基于 Vue 3、Vite、Vue Router、Pinia、Element Plus 和 Axios。

## 启动

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

## 后端

默认后端地址：

```text
http://localhost:8080
```

Vite 已配置 `/api` 代理到后端，Axios 默认使用相对路径 `/api`。
如果前后端部署在不同域名，可复制 `.env.example` 为 `.env.production`，并设置：

```text
VITE_API_BASE_URL=https://api.example.com/api
```

## 构建

```bash
npm run build
npm run preview
```

使用 `createWebHistory` 时，生产服务器需要把未知路由回退到 `index.html`。Nginx 示例：

```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```
