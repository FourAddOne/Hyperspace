-- auto-generated definition
create table `group`
(
    id          bigint unsigned auto_increment comment '数据库内部自增主键'
        primary key,
    group_id    varchar(64)                           not null comment '群组全局唯一ID（业务主键）',
    group_name  varchar(100)                          not null comment '群组名称',
    creator_id  varchar(64)                           not null comment '群组创建者ID（关联user.user_id）',
    members     json                                  null comment '群成员ID集合（JSON格式，冗余存储，用于快速查询成员数量）',
    member_count int unsigned default 0               not null comment '群成员数量（冗余字段，便于统计）',
    avatar_url  varchar(255)                          null comment '群头像URL地址',
    description varchar(500)                          null comment '群组描述介绍',
    created_at  datetime    default CURRENT_TIMESTAMP null comment '群组创建时间（自动生成）',
    updated_at  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '群组信息更新时间（自动更新）',
    status      varchar(20) default 'NORMAL'          null comment '群组状态（NORMAL-正常，MUTE_ALL-全体禁言，BANNED-封禁）',
    constraint uk_group_group_id
        unique (group_id)
)
    comment '群组基础信息表' collate = utf8mb4_unicode_ci;

create index idx_group_creator_id
    on `group` (creator_id);

create index idx_group_status
    on `group` (status);