# OSS CORS 配置说明

## 问题描述

前端直传OSS时出现以下错误：
```
Access to XMLHttpRequest at 'https://fourandone-hyperspace.oss-cn-hangzhou.aliyuncs.com/' from origin 'http://localhost:5173' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## 解决方案

需要在阿里云OSS控制台配置CORS规则，允许前端域名访问。

## 配置步骤

1. 登录阿里云控制台
2. 进入对象存储OSS管理页面
3. 找到您的存储桶（bucket）`fourandone-hyperspace`
4. 点击存储桶名称进入详情页面
5. 在左侧菜单中选择"权限管理" -> "跨域设置"
6. 点击"创建规则"
7. 填写以下配置：

### CORS规则配置

- **来源**：
  - `http://localhost:5173` （开发环境）
  - `http://localhost:8080` （如果使用其他端口）
  - 您的生产环境域名（例如：`https://yourdomain.com`）

- **允许Methods**：
  - `GET`
  - `POST`
  - `PUT`
  - `HEAD`
  - `DELETE`

- **允许Headers**：
  - `*` （通配符，允许所有头部）

- **暴露Headers**：
  - `ETag`
  - `Content-Length`
  - `x-oss-request-id`

- **缓存时间**：
  - `60` 秒

## 示例配置

```json
[
    {
        "AllowedOrigin": [
            "http://localhost:5173",
            "http://localhost:8080",
            "https://yourdomain.com"
        ],
        "AllowedMethod": [
            "GET",
            "POST",
            "PUT",
            "HEAD",
            "DELETE"
        ],
        "AllowedHeader": [
            "*"
        ],
        "ExposeHeader": [
            "ETag",
            "Content-Length",
            "x-oss-request-id"
        ],
        "MaxAgeSeconds": 60
    }
]
```

## 保存配置

配置完成后点击"确定"保存规则。通常需要几分钟时间生效。

## 验证配置

配置生效后，重新尝试文件上传功能，应该不会再出现CORS错误。