# 校园活动抢票平台（代码骨架）

> 这是《校园活动抢票平台-项目设计方案》配套的可运行骨架

## 技术栈

- Spring Boot 2.7 + MyBatis-Plus + MySQL 8
- Redis（抢票 Lua 预扣 + 热点缓存）
- Lombok

## 目录结构

```
src/main/java/com/ticket/platform/
├── TicketPlatformApplication.java    启动类
├── common/           Result、BizException、全局异常处理
├── entity/           User / Activity / ActivityTicket / Order / OrderLog
├── mapper/           五个 Mapper（含库存扣减 SQL）
├── state/            OrderStatus 状态机
├── service/          OrderService（抢票核心）、TicketService（缓存防击穿）
├── task/             OrderTimeoutTask（超时自动关单）
├── controller/       User / Activity / Admin / Order 接口
└── vo/               ActivityVO
src/main/resources/
├── application.yml
└── lua/              stock_dec.lua（原子扣减）、stock_inc.lua（回补）
```

## 核心流程（先看这里）

```
抢票 grabTicket()
  ① 计数器限流（可选）
  ② Redis Lua 原子预扣  ticket:stock:{ticketId}
  ③ 数据库乐观锁兜底    UPDATE activity_ticket SET stock=stock-1 WHERE id=? AND stock>=1
  ④ 插入订单（待支付）
  ⑤ 记录状态日志 order_log
  失败 → catch 里 Redis 回补库存，DB 由事务回滚

超时关单 OrderTimeoutTask（每 30s）
  WAIT_PAY 且超过 15 分钟 → 状态机校验 → 置为已取消 → DB 回补库存 + Redis 回补 → 记日志

缓存防击穿 TicketService.getActivityDetail()
  缓存 miss → 互斥锁（setIfAbsent）→ 只让一个请求重建 → 其他请求等 50ms 重试
```

## 建表 SQL（在 MySQL 里执行）

```sql
CREATE TABLE `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,   -- 登录名
  phone VARCHAR(20) NOT NULL UNIQUE,
  nickname VARCHAR(50),
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER 普通用户 / ADMIN 管理员(root)
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 如果是老库（已有 user 表），执行迁移：
-- ALTER TABLE `user` ADD COLUMN username VARCHAR(50) NULL UNIQUE AFTER id;
-- ALTER TABLE `user` ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
-- UPDATE `user` SET username = nickname WHERE username IS NULL;   -- 老用户直接用昵称当用户名
-- （若 nickname 为空或重复，再兜底：UPDATE `user` SET username = CONCAT('user_', id) WHERE username IS NULL;）

CREATE TABLE `activity` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  start_time DATETIME,
  location VARCHAR(200),
  detail TEXT,
  status TINYINT DEFAULT 0,        -- 0未开始 1进行中 2已结束
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `activity_ticket` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  name VARCHAR(50),
  price DECIMAL(10,2) DEFAULT 0,
  total_stock INT DEFAULT 0,
  stock INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `order` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  `count` INT DEFAULT 1,
  amount DECIMAL(10,2),
  status TINYINT DEFAULT 0,        -- 0待支付 1已支付 2已取消 3已核销 4已退款
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  pay_time DATETIME,
  UNIQUE KEY uk_user_activity (user_id, activity_id)   -- 防重复抢票（幂等）
);

CREATE TABLE `order_log` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status VARCHAR(20) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 运行步骤

1. 本地装好 JDK8+、MySQL 8、Redis
2. 执行上面的建表 SQL，库名 `ticket_platform`（老库先跑迁移语句）
3. 改 `application.yml` 里的数据库账号密码
4. `mvn spring-boot:run` 启动 —— 会自动创建 root 管理员：**用户名 `root` / 密码 `root123`**（ADMIN 角色，负责管理端活动发布）
5. 用 root 登录管理端，先发布一个活动 + 票种（会同步初始化 Redis 库存）
6. 前端（前后端分离）：`cd frontend && npm install && npm run dev`，访问 http://localhost:5173

> 登录说明：支持**用户名或手机号**二选一登录。注册的账号默认普通用户（USER），只能浏览活动、抢票、看自己的订单；管理端（发布活动、订单核销）仅 root 可见可操作。

## 前端

独立目录 `frontend/`（Vue3 + Vite + Element Plus）。
开发时 Vite 把 `/api` 代理到 8080，生产由 Nginx 静态托管 `dist/` 并反代 `/api`。详见 `frontend/README.md`。
