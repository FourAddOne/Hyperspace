create table "user"
(
    user_id      varchar                  default nextval('user_id_seq'::regclass) not null
        primary key,
    user_name    varchar(50)                                                       not null,
    email        varchar(100),
    avatar_url   text,
    password     varchar(100)                                                      not null,
    last_read_ts bigint                   default 0,
    login_status boolean                  default false,
    created_at   timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at   timestamp with time zone default CURRENT_TIMESTAMP,
    register_ip  varchar(39)              default '0.0.0.0'::character varying     not null,
    login_ip     varchar(39)
);

alter table "user"
    owner to postgres;

create table message
(
    id               bigserial
        primary key,
    message_id       varchar(64) not null
        unique,
    type             varchar(20) not null,
    from_user_id     varchar(64) not null,
    from_username    varchar(100),
    to_target_id     varchar(64) not null,
    to_target_type   varchar(20) not null,
    to_target_name   varchar(100),
    text_content     text,
    image_urls       text,
    client_timestamp bigint      not null,
    server_timestamp bigint      not null,
    status           varchar(20) not null,
    quote_message_id varchar(64),
    device_type      varchar(50),
    device_name      varchar(100),
    create_time      timestamp default CURRENT_TIMESTAMP,
    update_time      timestamp default CURRENT_TIMESTAMP,
    rich_content     text
);

alter table message
    owner to postgres;

create index idx_message_message_id
    on message (message_id);

create index idx_message_from_user_id
    on message (from_user_id);

create index idx_message_to_target_id
    on message (to_target_id);

create index idx_message_client_timestamp
    on message (client_timestamp);

create index idx_message_server_timestamp
    on message (server_timestamp);

create index idx_message_status
    on message (status);

create table "group"
(
    id         bigserial
        primary key,
    group_id   varchar(64)  not null
        unique,
    group_name varchar(100) not null,
    creator_id varchar(64)  not null,
    members    jsonb,
    created_at timestamp   default CURRENT_TIMESTAMP,
    updated_at timestamp   default CURRENT_TIMESTAMP,
    status     varchar(20) default 'NORMAL'::character varying
);

comment on sequence group_id_seq is '群组ID生成序列';

comment on column "group".status is '群组状态 (NORMAL: 正常, MUTE_ALL: 全体禁言, BANNED: 封禁)';

alter table "group"
    owner to postgres;

create table group_member
(
    id            bigserial
        primary key,
    group_id      varchar(64) not null,
    user_id       varchar(64) not null,
    role          varchar(20) default 'MEMBER'::character varying,
    joined_at     timestamp   default CURRENT_TIMESTAMP,
    updated_at    timestamp   default CURRENT_TIMESTAMP,
    status        varchar(20) default 'NORMAL'::character varying,
    mute_end_time bigint,
    unique (group_id, user_id)
);

comment on column group_member.status is '用户状态 (NORMAL: 正常, MUTED: 禁言)';

comment on column group_member.mute_end_time is '禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)';

alter table group_member
    owner to postgres;

create index idx_group_member_user_id
    on group_member (user_id);

create index idx_group_member_group_id
    on group_member (group_id);

create index idx_group_member_status
    on group_member (status);

create table friend
(
    id              bigserial
        primary key,
    user_one        varchar(64) not null,
    user_sec        varchar(64) not null,
    status          varchar(20) default 'PENDING'::character varying,
    block_status    varchar(20) default 'NONE'::character varying,
    user_one_remark varchar(100),
    user_sec_remark varchar(100),
    created_at      bigint      not null,
    updated_at      bigint      not null,
    unique (user_one, user_sec)
);

comment on column friend.user_one is '用户1ID（较小的用户ID）';

comment on column friend.user_sec is '用户2ID（较大的用户ID）';

comment on column friend.status is '好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)';

comment on column friend.block_status is '屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)';

comment on column friend.user_one_remark is '用户1对用户2的备注名称';

comment on column friend.user_sec_remark is '用户2对用户1的备注名称';

comment on column friend.created_at is '创建时间';

comment on column friend.updated_at is '更新时间';

alter table friend
    owner to postgres;

create index idx_friend_user_one
    on friend (user_one);

create index idx_friend_user_sec
    on friend (user_sec);

create index idx_friend_status
    on friend (status);

create index idx_friend_block_status
    on friend (block_status);

create index idx_friend_created_at
    on friend (created_at);

create index idx_friend_one_status_block
    on friend (user_one, status, block_status) include (user_sec, user_one_remark, user_sec_remark);

create index idx_friend_sec_status_block
    on friend (user_sec, status, block_status) include (user_one, user_one_remark, user_sec_remark);

