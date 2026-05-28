# 图雀 ---- 一个智能图库

一个集成多种 AI 能力的图库，支持语义搜索、AI 审核 Agent、自动标签识别、AI 扩图。

## 功能特性

### 核心功能

- **公共图库**：所有用户可公开上传和检索图片
- **私有空间**：个人用户可将图片上传至私有空间进行批量管理、多维检索、编辑和分析
- **团队空间**：企业可开通团队空间并邀请成员，共享和实时协同编辑图片
- **后台管理**：管理员可以上传、审核和管理分析图片

### AI 能力

- **AI 审核 Agent**：基于 LangChain4j AiServices + Tool Calling，大模型自主调用图片分析、安全检测工具，自动完成内容审核
- **AI 语义搜索**：text-embedding-v3 + Redis Stack HNSW 索引，图片描述转为 1024 维向量，支持自然语言检索
- **AI 自动标签**：集成阿里云多模态大模型（qwen-vl-plus），上传图片后自动识别标签和分类
- **AI 扩图**：阿里云图像外绘模型，异步任务模式（提交 → 轮询 → 获取结果）

## 技术栈

### 前端

| 技术               | 说明      |
| ------------------ | --------- |
| Vue 3              | 前端框架  |
| TypeScript         | 类型安全  |
| Ant Design Vue 4.x | UI 组件库 |
| Vite               | 构建工具  |
| Pinia              | 状态管理  |
| Vue Router         | 路由管理  |

### 后端

| 技术                    | 说明         |
| ----------------------- | ------------ |
| Spring Boot 2.7.6       | 后端框架     |
| MyBatis-Plus 3.5.15     | ORM 框架     |
| MySQL                   | 关系型数据库 |
| Redis + Caffeine        | 多级缓存     |
| ShardingSphere          | 分库分表     |
| Sa-Token 1.39.0         | 认证鉴权     |
| LangChain4j 1.0.0-beta1 | AI 应用框架  |
| 阿里云 DashScope        | 大模型 API   |
| 腾讯云 COS              | 对象存储     |
| WebSocket + Disruptor   | 实时通信     |
| Knife4j                 | 接口文档     |

## 项目结构

```
sr-picture/
├── sr-picture-backend/          # 后端 Spring Boot 项目
│   ├── src/main/java/com/tuque/srpicturebackend/
│   │   ├── controller/          # REST 接口层
│   │   ├── service/             # 业务逻辑层
│   │   │   └── impl/            # 业务实现
│   │   ├── manager/             # 底层能力封装
│   │   │   ├── upload/          # 文件上传（模板方法模式）
│   │   │   ├── sharding/        # 分表算法
│   │   │   ├── websocket/       # WebSocket 通信
│   │   │   └── auth/            # 权限校验
│   │   ├── mapper/              # 数据访问层
│   │   ├── model/               # 数据模型
│   │   │   ├── entity/          # 实体类
│   │   │   ├── dto/             # 请求参数
│   │   │   ├── vo/              # 响应对象
│   │   │   └── enums/           # 枚举
│   │   ├── config/              # 配置类
│   │   ├── api/                 # 外部 API 封装
│   │   │   └── aliyunai/        # 阿里云 AI 接口
│   │   └── aop/                 # 切面（权限校验）
│   └── src/main/resources/
│       └── application.yml      # 配置文件
│
└── sr-picture-frontend/         # 前端 Vue 3 项目
    ├── src/
    │   ├── pages/               # 页面组件
    │   ├── components/          # 公共组件
    │   ├── api/                 # API 客户端（自动生成）
    │   ├── stores/              # Pinia 状态管理
    │   ├── router/              # 路由配置
    │   └── access.ts            # 路由守卫
    └── package.json
```

## 快速开始

### 环境要求

- JDK 11+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+
- Redis Stack（语义搜索功能需要，可选）

### 后端启动

```bash
cd sr-picture-backend

# 1. 创建数据库
mysql -u root -p
CREATE DATABASE your-database;

# 2. 修改配置文件
#    复制 application.yml 中的配置到 application-local.yml
#    修改数据库连接、Redis 连接、阿里云 API Key、COS 配置

# 3. 构建并运行
mvn clean package -DskipTests
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8123/api`，接口文档地址：`http://localhost:8123/api/doc.html`

### 前端启动

```bash
cd sr-picture-frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`

### 启动 Redis Stack（可选，语义搜索功能需要）

```bash
# Docker 方式启动
docker run -d --name redis-stack -p 6379:6379 redis/redis-stack:latest
```

在 `application.yml` 中设置 `picture.search.vector-enabled: true` 启用语义搜索。

## 配置说明

### 后端配置（application.yml）

```yaml
# 数据库
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: youe_username
    password: your_password
  redis:
    host: 127.0.0.1
    port: 6379
    password: your_redis_password

# 腾讯云 COS
cos:
  client:
    host: your_host
    secretId: your_secret_id
    secretKey: your_secret_key
    region: your_host_region
    bucket: your_bucket_name

# 阿里云 AI
aliYunAi:
  apiKey: your_dashscope_api_key

# LangChain4j（AI Agent 使用）
langchain4j:
  community:
    dashscope:
      chat-model:
        api-key: your_dashscope_api_key
        model-name: qwen-plus

# 语义搜索开关
picture:
  search:
    vector-enabled: true   # true=启用，false=禁用
```

### 前端配置

前端 API 地址在 `src/request.ts` 中配置，默认指向 `http://localhost:8123/api`。

## 常用命令

### 后端

```bash
mvn clean package -DskipTests   # 构建
mvn spring-boot:run              # 运行
mvn test                         # 运行测试
```

### 前端

```bash
npm run dev          # 启动开发服务器
npm run build        # 生产构建
npm run type-check   # 类型检查
npm run lint         # ESLint 修复
npm run openapi      # 从后端重新生成 API 客户端
```

## 请求链路

```
前端请求 → Controller → Service → Manager → Mapper → MySQL
                                  ↓
                              CosManager → 腾讯云 COS
                              AliYunAiApi → 阿里云 DashScope
                              PictureSearchService → Redis Stack（向量）
```

## 权限模型

| 角色       | 权限                                           |
| ---------- | ---------------------------------------------- |
| 普通用户   | 上传图片到公共图库、管理私有空间、加入团队空间 |
| 管理员     | 审核图片、管理所有空间、后台管理               |
| 团队管理员 | 管理团队空间、邀请成员                         |
| 团队成员   | 在团队空间内上传和编辑图片                     |

## AI 功能原理

### AI 审核 Agent

```
图片上传 → Agent 收到审核请求
  → 调用 analyzeImage 工具（分析图片内容）
  → 调用 checkSafety 工具（检测敏感内容）
  → 大模型决策：调用 approvePicture 或 rejectPicture
  → 更新数据库审核状态
```

### AI 语义搜索

```
建索引：图片上传 → AI 生成描述 → text-embedding-v3 转向量 → 存入 Redis Stack
搜  索：用户输入 → 转向量 → Redis HNSW 检索 → 返回相似度 > 0.80 的结果
```
