# 延安三路站排班系统

地铁运营三中心站务排班管理系统，支持月班表 / 周班表上传、在线编辑及实时缺岗预警。

---

## 功能模块

| 模块 | 功能 |
|------|------|
| 账号登录 | JWT 鉴权，操作日志记录 |
| 班表上传 | 解析现有 Excel 格式（月/周），自动识别班次与人员分组 |
| 在线查看 | 可视化表格，颜色高亮班组 |
| 在线编辑 | 双击单元格修改班次，实时保存 |
| 实时预警 | A1/A2/C1/C2/F1/F2/E2 班次若当天人数为0，立即显示红色预警 |
| 操作日志 | 记录所有上传、编辑、删除操作（ADMIN可查） |

---

## 技术栈

- **前端**：Vue 3 + Vite + Element Plus + Pinia
- **后端**：Spring Boot 3 + Spring Security (JWT) + Spring Data JPA
- **数据库**：H2（开发） / MySQL（生产）
- **Excel解析**：Apache POI

---

## 快速启动

### 1. 后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`

**默认账号：**
- `admin` / `admin123`（管理员）
- `manager` / `manager123`（站区长）

H2 控制台：`http://localhost:8080/h2-console`

### 2. 前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`

---

## 切换 MySQL

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scheduling?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: yourpassword
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
```

同时在 `pom.xml` 中取消 mysql-connector-j 的注释并注释掉 h2。

---

## 预警规则

每天对 A1、A2、C1、C2、F1、F2、E2 七个岗位进行统计：
- **人数 = 1**：✅ 正常
- **人数 = 0**：🚨 缺岗预警，需安排替班

班表编辑后自动重新计算当天各岗位人数，预警实时更新。

---

## 班次说明

| 班次 | 角色 |
|------|------|
| A1 / A2 | 值班站长班次（规律：A1 A2 休 休） |
| C1 / C2 | 值班员班次（规律：C1 C2 休 休） |
| F1 / F2 / E2 | 站务员班次（各班组有各自规律） |
| 跟F1 / 跟A1 等 | 跟班，计入对应主班次 |
| 休 / 白 / 年 / 婚 | 非在岗状态，不计入班次人数 |

---

## 目录结构

```
scheduling-system/
├── backend/              # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/metro/scheduling/
│       │   ├── controller/     # REST API
│       │   ├── service/        # 业务逻辑
│       │   ├── entity/         # JPA 实体
│       │   ├── repository/     # 数据库访问
│       │   ├── util/           # JWT + Excel 解析
│       │   └── config/         # Security + 初始化
│       └── resources/
│           └── application.yml
│
└── frontend/             # Vue 3 前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── views/         # 页面组件
        ├── components/    # 通用组件
        ├── api/           # HTTP 请求
        ├── store/         # Pinia 状态
        └── router/        # 路由配置
```
