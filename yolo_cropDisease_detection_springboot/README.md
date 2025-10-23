
# 工程简介

本项目是基于 Spring Boot 的后端服务（artifactId: Kcsj）。默认编译目标为 Java 8，但项目已做了兼容性处理，可以在本机的 Java 17 环境下使用 Maven Wrapper 构建并运行。

## 快速开始（使用当前系统 Java 17）

1. 检查本地 Java 与 Maven Wrapper：

```powershell
java -version
.\mvnw.cmd -v
```

2. 使用 Maven Wrapper 构建（跳过测试）：

```powershell
.\mvnw.cmd -DskipTests package -U
```

3. 运行服务：

```powershell
.\mvnw.cmd spring-boot:run
# 或者运行打包后的 jar：
# java -jar target\Kcsj-0.0.1-SNAPSHOT.jar
```

4. 配置（重要）

- 数据库连接配置位于 `src/main/resources/application.properties`：
	- `spring.datasource.url=jdbc:mysql://localhost:3306/cropdisease?serverTimezone=Asia/Shanghai`
	- `spring.datasource.username=root`
	- `spring.datasource.password=123456`

	请根据你的数据库实际情况修改并确保数据库可连通。

## 常见问题与排查

- Lombok 在 JDK9+ 报 IllegalAccessError：
	- 项目已在 `pom.xml` 增加 `jdk9-plus` profile（当 JDK >= 9 时自动激活），为 Lombok 添加注解处理器路径并设置 `--add-opens`。
	- 在 IDE（如 IntelliJ IDEA）中，建议安装 Lombok 插件并在项目设置中启用注解处理器（Annotation Processors）。

- 构建/依赖下载缓慢或失败：
	- 检查网络是否能访问 Maven 中央仓库，或配置公司内部仓库/代理。可用 `-U` 强制更新依赖缓存。

## 开发小贴士

- 当前可在 Java 17 上直接构建与运行（我在本机验证过通过 `mvnw.cmd -DskipTests package -U` 构建成功）。
- 推荐使用 Maven Wrapper (`mvnw.cmd`) 以保证构建时使用与项目一致的 Maven 版本。

---

延伸阅读与配置说明请参考项目中的 `application.properties`。

## 开发者上手指南

下面内容面向开发者，包含数据库初始化 SQL、常用 API 列表及一个 `docker-compose` 示例（用于快速启动 MySQL 与示例 Flask 服务）。

### 1) 数据库初始化（MySQL）

在 MySQL 中执行下面的 SQL（示例数据库名：`cropdisease`）：

```sql
CREATE DATABASE IF NOT EXISTS `cropdisease` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `cropdisease`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
	`id` INT AUTO_INCREMENT PRIMARY KEY,
	`username` VARCHAR(100) NOT NULL UNIQUE,
	`password` VARCHAR(255) NOT NULL,
	`name` VARCHAR(100),
	`sex` VARCHAR(10),
	`email` VARCHAR(150),
	`tel` VARCHAR(50),
	`role` VARCHAR(50),
	`avatar` VARCHAR(512),
	`time` DATETIME
);

-- 图像记录
CREATE TABLE IF NOT EXISTS `imgrecords` (
	`id` INT AUTO_INCREMENT PRIMARY KEY,
	`weight` VARCHAR(100),
	`inputImg` TEXT,
	`outImg` TEXT,
	`confidence` VARCHAR(100),
	`allTime` VARCHAR(100),
	`conf` VARCHAR(100),
	`label` VARCHAR(200),
	`username` VARCHAR(100),
	`kind` VARCHAR(100),
	`startTime` VARCHAR(100)
);

-- 摄像头记录
CREATE TABLE IF NOT EXISTS `camerarecords` (
	`id` INT AUTO_INCREMENT PRIMARY KEY,
	`weight` VARCHAR(100),
	`outVideo` TEXT,
	`conf` VARCHAR(100),
	`username` VARCHAR(100),
	`kind` VARCHAR(100),
	`startTime` VARCHAR(100)
);

-- 视频记录
CREATE TABLE IF NOT EXISTS `videorecords` (
	`id` INT AUTO_INCREMENT PRIMARY KEY,
	`weight` VARCHAR(100),
	`inputVideo` TEXT,
	`outVideo` TEXT,
	`conf` VARCHAR(100),
	`username` VARCHAR(100),
	`kind` VARCHAR(100),
	`startTime` VARCHAR(100)
);

-- 示例：插入一个管理员用户
INSERT INTO `user` (username, password, name, role, email, time) VALUES ('admin', 'admin123', '管理员', 'admin', 'admin@example.com', NOW());
```

> 注意：示例密码为明文，生产环境请务必使用哈希（如 bcrypt）。

### 2) 常用 API 列表（摘自 Controller）

- 用户管理（`/user`）
	- GET /user?pageNum=&pageSize=&search=  -> 分页查询用户列表
	- GET /user/{username}  -> 按用户名查询用户
	- GET /user/all  -> 查询所有用户
	- POST /user/login  -> 登录（请求体：{username,password}）
	- POST /user/register  -> 注册（请求体：User 对象）
	- POST /user/update  -> 更新用户（请求体：User 对象）
	- DELETE /user/{id}  -> 删除用户
	- POST /user  -> 新增用户（请求体：User 对象）

- 图像预测与记录（`/flask`, `/imgRecords`）
	- POST /flask/predict  -> 发起预测请求（请求体：{username,startTime,weight,inputImg,kind,conf,weight}），服务会调用本地 Flask API（默认 http://localhost:5000/predictImg），并将结果保存到 `imgrecords`。
	- GET /flask/file_names  -> 获取 Flask 服务的文件名列表（代理到 Flask: /file_names）
	- GET /imgRecords  -> 分页查询图像记录（支持多字段搜索）
	- GET /imgRecords/{id}  -> 根据 id 查询图像记录
	- GET /imgRecords/all  -> 查询所有图像记录
	- POST /imgRecords  -> 新增图像记录
	- POST /imgRecords/update  -> 更新图像记录
	- DELETE /imgRecords/{id}  -> 删除图像记录

- 文件上传/下载（`/files`）
	- POST /files/upload  -> 上传文件，返回访问 URL（multipart/form-data）
	- POST /files/editor/upload  -> 编辑器富文本上传（返回特定 JSON 格式）
	- GET /files/{flag}  -> 根据 flag 下载文件

- 摄像头与视频记录（`/cameraRecords`, `/videoRecords`）
	- GET /cameraRecords  -> 分页查询摄像头记录
	- GET /cameraRecords/{id}  -> 查询单条记录
	- GET /cameraRecords/all  -> 查询所有
	- POST /cameraRecords  -> 新增
	- POST /cameraRecords/update  -> 更新
	- DELETE /cameraRecords/{id}  -> 删除
	- videoRecords 同上（接口路径为 `/videoRecords`）

> 注意：预测功能会调用本地 Flask 服务（项目通过 `PredictionController` 调用 http://localhost:5000/predictImg）。若你使用 docker-compose 启动 Flask，请确保将地址与端口对应修改。

### 3) docker-compose 示例（快速启动 MySQL 与一个占位的 Flask 服务）

下面示例适用于开发环境：

```yaml
version: '3.8'
services:
	db:
		image: mysql:5.7
		restart: unless-stopped
		environment:
			MYSQL_ROOT_PASSWORD: 123456
			MYSQL_DATABASE: cropdisease
			MYSQL_USER: root
			MYSQL_PASSWORD: 123456
		ports:
			- "3306:3306"
		volumes:
			- db_data:/var/lib/mysql

	flask_app:
		image: tiangolo/uwsgi-nginx-flask:python3.8
		restart: unless-stopped
		environment:
			- LISTEN_PORT=5000
		ports:
			- "5000:5000"
		volumes:
			- ./flask_app:/app  # 假设你的 Flask 服务代码位于项目根的 flask_app 目录

volumes:
	db_data:
```

在启动后：
- 等待 MySQL 启动并初始化（可使用 `docker-compose logs -f db` 查看）。
- 在容器内执行上面的数据库初始化 SQL（或在宿主机使用 `mysql` 客户端连接 db 容器执行）。

### 4) 本地开发建议
- 在 IDE 中启用 Lombok 注解处理器（Annotation Processors）。
- 若你需要调试预测流程，先确保 Flask 服务（在本地或容器中）可用并返回正确 JSON 结构。
- 生产环境请不要使用明文密码，使用加密存储与安全的数据库凭据管理。


