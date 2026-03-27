# Hyperspace
一款基于**前后端分离架构**开发的项目，采用Java生态后端与前端工程化开发模式，提供通用的项目开发基础框架，支持快速二次开发与业务功能拓展。

## 项目介绍
Hyperspace是一套轻量级的前后端分离开发模板，内置基础的项目工程结构、依赖管理与文件上传基础能力，适用于快速搭建各类Web应用。项目已完成基础工程初始化，包含前端工程目录、后端Maven工程配置、文件上传目录等核心模块，开发者可基于此框架快速实现业务逻辑开发。

## 技术栈
### 后端
- 开发框架：Spring Boot（Maven构建，基于`pom.xml`配置）
- 构建工具：Maven（内置`mvnw`/`mvnw.cmd`一键构建脚本）
- 基础能力：文件上传（`uploads`目录）、多环境配置

### 前端
- 工程结构：独立前端目录`hyperspace-master-front`
- 包管理：npm（基于`package.json`/`package-lock.json`）
- 开发模式：工程化前端开发，支持模块化、组件化开发

## 项目目录结构
```
Hyperspace/
├── .mvn/               # Maven wrapper 配置目录
├── hyperspace-master-front/  # 前端工程主目录
├── node_modules/       # 前端依赖包目录
├── src/                # 后端Java源码主目录
├── uploads/            # 文件上传存储目录
├── .gitattributes      # Git属性配置文件
├── .gitignore          # Git忽略文件配置
├── mvnw/mvnw.cmd       # Maven一键构建脚本（跨平台）
├── package-lock.json   # 前端依赖锁定文件
├── package.json        # 前端项目配置与依赖管理
├── pom.xml             # 后端Maven项目核心配置文件
```

## 环境要求
### 后端环境
- JDK 1.8+/11+（推荐11）
- Maven 3.6+ 或直接使用项目内置`mvnw`脚本
- 操作系统：Windows/Linux/MacOS（跨平台）

### 前端环境
- Node.js 14+/16+（推荐16）
- npm 6+ 或 yarn/pnpm（兼容主流包管理工具）

## 快速开始
### 1. 克隆项目
```bash
# 克隆仓库到本地
git clone https://github.com/FourAddOne/Hyperspace.git
# 进入项目根目录
cd Hyperspace
```

### 2. 后端启动
#### 方式1：使用内置Maven wrapper（推荐，无需手动安装Maven）
```bash
# Windows
mvnw.cmd spring-boot:run
# Linux/MacOS
./mvnw spring-boot:run
```

#### 方式2：本地已安装Maven
```bash
mvn clean install
mvn spring-boot:run
```

### 3. 前端启动
```bash
# 进入前端工程目录
cd hyperspace-master-front
# 安装前端依赖
npm install
# 启动前端开发服务
npm run dev # 或根据package.json配置的启动命令，如npm run serve
```

## 核心功能
1. **基础工程骨架**：已完成前后端项目初始化，无需重复搭建基础结构
2. **文件上传基础**：内置`uploads`目录，已完成文件存储基础配置，可直接拓展文件上传业务
3. **跨平台构建**：后端提供`mvnw`跨平台构建脚本，前端基于npm实现跨平台开发
4. **版本控制兼容**：已配置`.gitignore`/`.gitattributes`，适配Git版本管理

## 开发规范
### 分支管理
- 主分支：`main`/`master`（生产环境稳定代码）
- 开发分支：`dev`（日常开发集成）
- 功能分支：`feature/xxx`（单个功能开发，完成后合并至dev）
- 修复分支：`bugfix/xxx`（线上bug修复，完成后合并至main+dev）

### 提交规范
```
<type>: <subject>
```
- type：提交类型（feat/修复/docs/样式/重构/测试/构建）
- subject：提交描述，简洁说明修改内容

示例：
```
feat: 新增文件上传接口
fix: 修复前端请求跨域问题
docs: 更新README.md快速开始步骤
```

### 目录开发规范
- 后端：`src`目录下遵循Spring Boot标准MVC结构（controller/service/mapper/entity）
- 前端：`hyperspace-master-front`内遵循前端框架规范（如Vue的components/pages/router/store）
- 上传文件：所有业务上传文件统一存入`uploads`目录，按业务模块子目录划分

## 常见问题
1. **Maven启动失败**：检查JDK环境变量是否配置，或直接使用项目内置`mvnw`脚本
2. **前端依赖安装失败**：删除`node_modules`和`package-lock.json`，重新执行`npm install`，或切换npm镜像源
3. **文件上传权限问题**：确保`uploads`目录拥有读写权限，Linux/MacOS可执行`chmod 755 uploads`
4. **前后端跨域**：后端需配置CORS跨域允许，前端可通过代理配置解决跨域问题

## 项目维护
- 最后更新时间：2025年11月21日
- 核心提交记录：工程初始化、前端回滚、分支合并（LH分支）、依赖版本锁定

## 许可证
本项目采用**MIT License**开源协议，允许自由使用、修改、分发，商业/非商业项目均可基于此框架二次开发。

## 贡献
欢迎提交Issue和Pull Request进行功能拓展、Bug修复，提交前请遵循项目开发规范，确保代码风格统一、测试通过。
