# 前端直传OSS功能说明

## 功能概述

为了提升文件上传性能并降低后端服务器负载，我们实现了前端直传OSS功能。该功能允许前端直接将文件上传到阿里云OSS，而无需经过后端服务器中转。

## 实现原理

1. 前端向后端请求OSS上传策略
2. 后端生成临时上传凭证并返回给前端
3. 前端使用凭证直接上传文件到OSS
4. OSS返回上传结果，前端获取文件URL

## 文件类型支持

- 头像文件 (avatar)
- 背景图片 (background)
- 聊天图片 (message)
- 聊天文件 (file)

## 配置说明

通过环境变量 `VITE_OSS_DIRECT_UPLOAD` 控制是否启用直传功能：
- `true`: 启用直传OSS功能
- `false`: 使用原有的后端上传方式

## 代码结构

### 后端

1. `OssUtil.java` - 添加了生成上传策略的方法
2. `OssController.java` - 新增获取OSS上传策略的接口
3. `OssPolicyDTO.java` - OSS策略数据传输对象

### 前端

1. `src/services/api.ts` - 添加了直传OSS的核心函数
2. `src/constants/api.ts` - 添加了OSS相关API端点
3. `src/types/oss.ts` - OSS相关类型定义
4. `src/views/UserProfileView.vue` - 更新了头像和背景图片上传逻辑
5. `src/views/ChatView.vue` - 更新了聊天文件和图片上传逻辑

## 环境配置

在以下环境配置文件中可以控制是否启用直传功能：
- `.env` - 通用配置
- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置

## 优势

1. **性能提升**：文件直接上传到OSS，减少后端服务器压力
2. **用户体验**：上传速度更快，响应更迅速
3. **成本降低**：减少服务器带宽消耗
4. **扩展性强**：可以轻松应对用户量和文件量的增长

## 兼容性

为了保证功能的稳定性，原有的后端上传方式被完整保留作为备选方案。当直传OSS功能出现问题时，可以快速切换回原有方式。