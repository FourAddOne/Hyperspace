# 系统数据库设计文档
### 表结构总览
| 表名            | 核心作用                                  | 关联表               | 核心场景               |
|-----------------|-------------------------------------------|----------------------|------------------------|
| `user`          | 存储用户基础信息、登录状态、认证信息       | -                    | 用户注册、登录、资料展示 |
| `message`       | 统一存储单聊/群聊各类消息（文本、图片等） | 关联 `user`          | 消息收发、历史记录查询 |
| `group`         | 存储群组基础信息（名称、创建者、状态等）   | 关联 `user`          | 群组创建、状态管理     |
| `group_member`  | 存储群组与用户的关联关系（成员、角色、权限） | 关联 `group`、`user` | 群成员管理、权限控制   |
| `friend`        | 存储用户好友关系（状态、备注、屏蔽信息）   | 关联 `user`（双向）  | 好友申请、关系管理     |

## 二、详细表结构设计
### 1. 用户表（user）
| 字段名         | 数据类型                | 长度   | 允许空 | 默认值                                  | 注释说明                                                                 |
|----------------|-------------------------|--------|--------|-----------------------------------------|--------------------------------------------------------------------------|
| user_id        | VARCHAR                 | -      | 否     | nextval('user_id_seq'::regclass)        | 用户唯一ID（业务主键，通过序列自动生成）                                |
| user_name      | VARCHAR                 | 50     | 否     | -                                       | 用户名（显示用，非唯一）                                                 |
| email          | VARCHAR                 | 100    | 是     | -                                       | 用户邮箱（可用于登录，建议唯一）                                         |
| avatar_url     | TEXT                    | -      | 是     | -                                       | 用户头像URL                                                              |
| password       | VARCHAR                 | 100    | 否     | -                                       | 密码（存储加密后的哈希值，如BCrypt）                                     |
| last_read_ts   | BIGINT                  | -      | 是     | 0                                        | 最后读取消息的时间戳（毫秒级，用于未读消息计算）                         |
| login_status   | BOOLEAN                 | -      | 是     | false                                    | 登录状态（false-离线，true-在线）                                        |
| created_at     | TIMESTAMP WITH TIME ZONE | -      | 是     | CURRENT_TIMESTAMP                       | 注册时间（带时区，自动生成）                                             |
| updated_at     | TIMESTAMP WITH TIME ZONE | -      | 是     | CURRENT_TIMESTAMP                       | 信息更新时间（带时区，自动更新）                                         |
| register_ip    | VARCHAR                 | 39     | 否     | '0.0.0.0'::character varying             | 注册IP（支持IPv6，最长39位）                                             |
| login_ip       | VARCHAR                 | 39     | 是     | -                                       | 最后登录IP                                                              |



### 2. 消息表（message）
| 字段名             | 数据类型         | 长度   | 允许空 | 默认值                          | 注释说明                                                                 |
|--------------------|------------------|--------|--------|---------------------------------|--------------------------------------------------------------------------|
| id                 | BIGSERIAL        | -      | 否     | 自增生成                        | 数据库内部自增主键                                                       |
| message_id         | VARCHAR          | 64     | 否     | -                               | 消息全局唯一ID（业务主键，如：msg_${UUID}），唯一约束防止重复             |
| type               | VARCHAR          | 20     | 否     | -                               | 消息类型（text-文本、image-图片、quote-引用、system-系统通知等）          |
| from_user_id       | VARCHAR          | 64     | 否     | -                               | 发送者用户ID（关联 `user.user_id`）                                       |
| from_username      | VARCHAR          | 100    | 是     | -                               | 发送者用户名（冗余存储，优化展示性能）                                   |
| to_target_id       | VARCHAR          | 64     | 否     | -                               | 接收目标ID：群聊=群组ID，单聊=对方用户ID                                 |
| to_target_type     | VARCHAR          | 20     | 否     | -                               | 接收目标类型（group-群聊、user-单聊）                                     |
| to_target_name     | VARCHAR          | 100    | 是     | -                               | 接收目标名称（冗余存储：群聊=群名，单聊=对方用户名）                       |
| text_content       | TEXT             | -      | 是     | -                               | 文本消息内容（type为text/quote时必填）                                    |
| image_urls         | TEXT             | -      | 是     | -                               | 图片URL集合（JSON格式，如：["https://xxx/1.jpg"]，type=image时必填）       |
| client_timestamp   | BIGINT           | -      | 否     | -                               | 客户端发送时间戳（毫秒级，保障时序性）                                   |
| server_timestamp   | BIGINT           | -      | 否     | -                               | 服务器接收时间戳（毫秒级，存储排序基准）                                 |
| status             | VARCHAR          | 20     | 否     | -                               | 消息状态（sending-发送中、success-成功、failed-失败、recall-已撤回）       |
| quote_message_id   | VARCHAR          | 64     | 是     | -                               | 引用消息ID（关联本表 `message_id`，无引用则为空）                        |
| device_type        | VARCHAR          | 50     | 是     | -                               | 发送端设备类型（ios、android、web、windows等）                           |
| device_name        | VARCHAR          | 100    | 是     | -                               | 发送端设备名称（如：iPhone 15、Chrome浏览器）                             |
| create_time        | TIMESTAMP        | -      | 是     | CURRENT_TIMESTAMP               | 记录创建时间（自动生成）                                                 |
| update_time        | TIMESTAMP        | -      | 是     | CURRENT_TIMESTAMP               | 记录更新时间（自动更新）                                                 |
| rich_content       | TEXT             | -      | 是     | -                               | 富文本内容（JSON格式，存储带格式文本、表情、链接卡片等）                 |

#### 索引设计
| 索引名称                | 索引类型 | 索引字段               | 作用说明                                 |
|-------------------------|----------|------------------------|------------------------------------------|
| idx_message_message_id  | 普通索引 | message_id             | 快速查询指定业务主键的消息               |
| idx_message_from_user_id | 普通索引 | from_user_id           | 快速查询某用户发送的所有消息             |
| idx_message_to_target_id | 普通索引 | to_target_id           | 快速查询某目标（群/单聊）的所有消息       |
| idx_message_client_timestamp | 普通索引 | client_timestamp       | 按客户端时间排序查询消息                 |
| idx_message_server_timestamp | 普通索引 | server_timestamp       | 按服务器时间筛选/排序消息                |
| idx_message_status      | 普通索引 | status                 | 筛选特定状态的消息（如：已撤回、发送失败） |


### 3. 群组表（group）
| 字段名         | 数据类型         | 长度   | 允许空 | 默认值                                  | 注释说明                                                                 |
|----------------|------------------|--------|--------|-----------------------------------------|--------------------------------------------------------------------------|
| id             | BIGSERIAL        | -      | 否     | 自增生成                                | 数据库内部自增主键                                                       |
| group_id       | VARCHAR          | 64     | 否     | -                                       | 群组全局唯一ID（业务主键，如：group_${UUID}），唯一约束防止重复           |
| group_name     | VARCHAR          | 100    | 否     | -                                       | 群组名称                                                                 |
| creator_id     | VARCHAR          | 64     | 否     | -                                       | 群组创建者ID（关联 `user.user_id`）                                       |
| members        | JSONB            | -      | 是     | -                                       | 群成员ID集合（JSONB格式，冗余存储，用于快速查询群成员数量）               |
| created_at     | TIMESTAMP        | -      | 是     | CURRENT_TIMESTAMP                       | 群组创建时间（自动生成）                                                 |
| updated_at     | TIMESTAMP        | -      | 是     | CURRENT_TIMESTAMP                       | 群组信息更新时间（自动更新）                                             |
| status         | VARCHAR          | 20     | 是     | 'NORMAL'::character varying             | 群组状态（NORMAL-正常，MUTE_ALL-全体禁言，BANNED-封禁）                   |



### 4. 群成员表（group_member）
| 字段名         | 数据类型         | 长度   | 允许空 | 默认值                                  | 注释说明                                                                 |
|----------------|------------------|--------|--------|-----------------------------------------|--------------------------------------------------------------------------|
| id             | BIGSERIAL        | -      | 否     | 自增生成                                | 数据库内部自增主键                                                       |
| group_id       | VARCHAR          | 64     | 否     | -                                       | 关联 `group.group_id`（所属群组ID）                                       |
| user_id        | VARCHAR          | 64     | 否     | -                                       | 关联 `user.user_id`（成员用户ID）                                         |
| role           | VARCHAR          | 20     | 是     | 'MEMBER'::character varying             | 成员角色（MEMBER-普通成员，ADMIN-管理员，OWNER-群主）                     |
| joined_at      | TIMESTAMP        | -      | 是     | CURRENT_TIMESTAMP                       | 加入群组时间（自动生成）                                                 |
| updated_at     | TIMESTAMP        | -      | 是     | CURRENT_TIMESTAMP                       | 信息更新时间（如角色变更、禁言状态更新，自动更新）                       |
| status         | VARCHAR          | 20     | 是     | 'NORMAL'::character varying             | 成员状态（NORMAL-正常，MUTED-禁言）（注释：用户状态 (NORMAL: 正常, MUTED: 禁言)） |
| mute_end_time  | BIGINT           | -      | 是     | -                                       | 禁言结束时间戳（毫秒级，NULL=未禁言，-1=永久禁言）（注释：禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)） |

#### 约束与索引
| 约束/索引名称               | 类型           | 关联字段           | 作用说明                                  |
|-----------------------------|----------------|--------------------|-------------------------------------------|
| unique (group_id, user_id)  | 唯一约束       | group_id, user_id  | 避免用户重复加入同一群组                  |
| idx_group_member_user_id    | 普通索引       | user_id            | 快速查询用户加入的所有群组                |
| idx_group_member_group_id   | 普通索引       | group_id           | 快速查询群组的所有成员                    |
| idx_group_member_status     | 普通索引       | status             | 快速筛选禁言/正常状态的成员                |


### 5. 好友表（friend）
| 字段名             | 数据类型         | 长度   | 允许空 | 默认值                                  | 注释说明                                                                 |
|--------------------|------------------|--------|--------|-----------------------------------------|--------------------------------------------------------------------------|
| id                 | BIGSERIAL        | -      | 否     | 自增生成                                | 数据库内部自增主键                                                       |
| user_one           | VARCHAR          | 64     | 否     | -                                       | 用户1ID（约定存储较小的用户ID，避免重复关系）（注释：用户1ID（较小的用户ID）） |
| user_sec           | VARCHAR          | 64     | 否     | -                                       | 用户2ID（约定存储较大的用户ID）（注释：用户2ID（较大的用户ID））           |
| status             | VARCHAR          | 20     | 是     | 'PENDING'::character varying             | 好友状态（PENDING-等待确认，ACCEPTED-已接受，REJECTED-已拒绝，BLOCKED-已屏蔽）（注释：好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)） |
| block_status       | VARCHAR          | 20     | 是     | 'NONE'::character varying                | 屏蔽状态（NONE-无屏蔽，USER_ONE_BLOCKED-用户1屏蔽用户2，USER_SEC_BLOCKED-用户2屏蔽用户1，BOTH_BLOCKED-双方互相屏蔽）（注释：屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)） |
| user_one_remark    | VARCHAR          | 100    | 是     | -                                       | 用户1对用户2的备注名称（注释：用户1对用户2的备注名称）                     |
| user_sec_remark    | VARCHAR          | 100    | 是     | -                                       | 用户2对用户1的备注名称（注释：用户2对用户1的备注名称）                     |
| created_at         | BIGINT           | -      | 否     | -                                       | 关系创建时间戳（毫秒级）（注释：创建时间）                               |
| updated_at         | BIGINT           | -      | 否     | -                                       | 关系更新时间戳（毫秒级，如状态变更时间）（注释：更新时间）                 |

#### 约束与索引
| 约束/索引名称               | 类型           | 关联字段                                   | 作用说明                                  |
|-----------------------------|----------------|--------------------------------------------|-------------------------------------------|
| unique (user_one, user_sec) | 唯一约束       | user_one, user_sec                          | 避免重复建立好友关系（按ID大小存储，确保唯一） |
| idx_friend_user_one         | 普通索引       | user_one                                    | 快速查询用户1的所有好友关系                |
| idx_friend_user_sec         | 普通索引       | user_sec                                    | 快速查询用户2的所有好友关系                |
| idx_friend_status           | 普通索引       | status                                      | 按好友状态筛选（如：待确认的申请）          |
| idx_friend_block_status     | 普通索引       | block_status                                | 按屏蔽状态筛选（如：查询被屏蔽的好友）      |
| idx_friend_created_at       | 普通索引       | created_at                                  | 按创建时间排序查询（如：最近添加的好友）    |
| idx_friend_one_status_block | 复合索引       | user_one, status, block_status              | 多条件查询用户1的好友关系（包含备注字段）   |
| idx_friend_sec_status_block | 复合索引       | user_sec, status, block_status              | 多条件查询用户2的好友关系（包含备注字段）   |


