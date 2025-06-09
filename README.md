# Gobang 五子棋对战平台

## 项目简介

本项目是一个基于 Spring Boot + MyBatis + WebSocket 的五子棋对战平台，支持用户注册、登录、实时匹配、在线对战、积分统计等功能。前端采用原生 HTML/CSS/JS 实现，界面简洁，交互流畅，适合学习和二次开发。

---

## 目录结构

```
Gobang/
├── src/
│   ├── main/
│   │   ├── java/cn/iocoder/gobang/
│   │   │   ├── api/           # WebSocket 相关接口（MatchAPI、GameAPI等）
│   │   │   ├── controller/    # REST API 控制器（UserController）
│   │   │   ├── game/          # 核心业务逻辑（房间、匹配、在线用户管理等）
│   │   │   ├── mapper/        # MyBatis Mapper 接口
│   │   │   ├── model/         # 实体类（User等）
│   │   │   └── config/        # 配置类（WebSocket配置等）
│   │   ├── resources/
│   │   │   ├── static/
│   │   │   │   ├── css/       # 样式文件
│   │   │   │   ├── js/        # 前端脚本
│   │   │   │   ├── image/     # 图片资源
│   │   │   │   ├── login.html
│   │   │   │   ├── register.html
│   │   │   │   ├── game_hall.html
│   │   │   │   ├── game_room.html
│   │   │   ├── mapper/        # MyBatis XML 映射文件
│   │   │   ├── application.yml# 配置文件
│   │   │   ├── database.sql   # 数据库初始化脚本
├── pom.xml                    # Maven 构建文件
```

---

## 主要功能

- 用户注册、登录、会话管理
- 游戏大厅实时匹配
- WebSocket 实时对战
- 棋盘绘制与落子交互
- 对局积分统计
- 防止多开、断线重连处理

---

## 环境要求

- JDK 17
- Maven 3.6+
- MySQL 5.7/8.0
- 浏览器（推荐 Chrome）

---

## 快速启动

### 1. 数据库初始化

1. 创建数据库并导入表结构和初始数据：

   ```sql
   source src/main/resources/database.sql
   ```

2. 修改 `src/main/resources/application.yml`，配置你的 MySQL 账号和密码：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/java_gobang?allowPublicKeyRetrieval=true&useSSL=false
       username: root
       password: 你的数据库密码
   ```

### 2. 启动后端服务

```bash
mvn clean package
# 运行主类
java -jar target/Gobang-0.0.1-SNAPSHOT.jar
# 或直接用IDEA运行 GobangApplication.java
```

### 3. 访问前端页面

- 登录页：http://localhost:8080/login.html
- 注册页：http://localhost:8080/register.html
- 游戏大厅：http://localhost:8080/game_hall.html
- 游戏房间：http://localhost:8080/game_room.html

---

## 主要技术点

- **Spring Boot**：后端主框架，简化配置和部署
- **MyBatis**：数据库访问层，配合 XML 映射文件
- **WebSocket**：实现实时匹配和对战
- **原生 JS/HTML/CSS**：前端页面与交互
- **多线程与同步**：后端匹配队列、房间管理
- **断线重连与多开检测**：提升用户体验和系统健壮性

---

## 重要文件说明

- `src/main/java/cn/iocoder/gobang/api/MatchAPI.java`：大厅匹配 WebSocket 处理
- `src/main/java/cn/iocoder/gobang/api/GameAPI.java`：游戏房间 WebSocket 处理
- `src/main/java/cn/iocoder/gobang/game/Matcher.java`：匹配线程与队列管理
- `src/main/java/cn/iocoder/gobang/game/Room.java`：房间与对局逻辑
- `src/main/java/cn/iocoder/gobang/controller/UserController.java`：用户注册、登录、信息获取
- `src/main/resources/static/js/script.js`：前端棋盘与对战逻辑
- `src/main/resources/database.sql`：数据库表结构与初始数据

---

## 常见问题

- **无法连接数据库**：请检查 `application.yml` 的数据库配置和 MySQL 服务状态。
- **WebSocket 连接失败**：请确保前后端端口一致，且浏览器支持 WebSocket。
- **匹配后无法进入房间**：请确保两个账号都正常登录并匹配，且未多开。
- **按钮样式不美观**：可在 `css/common.css` 或相关 CSS 文件中自定义样式。

---

## 贡献与反馈

如有建议、bug 或想参与开发，欢迎提 issue 或 pull request！

---

## License

本项目仅供学习交流使用，禁止用于商业用途。

---

如需更详细的开发文档或二次开发指导，请联系作者或提交 issue。 