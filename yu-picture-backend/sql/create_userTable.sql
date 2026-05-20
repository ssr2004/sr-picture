-- 用户表
create table if not exists user
(
    id bigint auto_increment comment 'id' primary key ,
    userAccount varchar(256) not null comment '账号',
    userPassword varchar(512) not null comment '密码',
    userName varchar(256) not null comment '用户昵称',
    userAvatar varchar(1024) not null comment '用户头像',
    userProfile varchar(512) not null comment '用户简介',
    userRole varchar(256) default 'user' comment '用户角色：user/admin',
    editTime datetime default current_timestamp not null comment '编辑时间',
    createTime datetime default current_timestamp not null comment '创建时间',
    updateTime datetime default current_timestamp on update current_timestamp not null comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',
    unique key uk_userAccount (userAccount),
    index idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;


-- 图片表
create table if not exists picture
(
    id    bigint auto_increment comment 'id' primary key ,
    url   varchar(512) not null comment '图片url',
    name  varchar(128) not null comment '图片名称',
    introduction  varchar(512)  null comment '简介',
    category  varchar(64) null comment '分类',
    tags  varchar(512)   null comment '标签(JOSN数组)',
    picSize  bigint  null comment '图片体积',
    picWidth  int null comment '图片宽度',
    picHeight  int null comment '图片高度',
    picScale double null comment '图片宽高比例',
    picFormat varchar(32) null comment '图片格式',
    userId bigint not null comment '创建用户id',
    createTime datetime default current_timestamp not null comment '创建时间',
    editTime datetime default current_timestamp  not null comment '编辑时间',
    updateTime datetime default current_timestamp on update current_timestamp not null comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',

    index idx_name (name), -- 提升基于图片名称的查询性能
    index idx_introduction (introduction), -- 用于模糊搜索图片简介
    index idx_category (category), -- 提升基于分类的查询性能
    index idx_tags (tags), -- 提升基于标签的查询性能
    index idx_userId (userId) -- 提升基于用户ID的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

alter table picture
    add column reviewStatus int default 0 not null comment '审核状态：0-待审核，1-通过，2-拒绝',
    add column reviewMessage varchar(512) null comment '审核信息',
    add column reviewId bigint null comment '审核人ID',
    add column reviewTime datetime null comment '审核时间';

-- 创建基于 reviewStatus 列的索引
create index idx_reviewStatus on picture (reviewStatus);

-- 增加缩略图URL字段
alter table picture
    add column thumbnailUrl varchar(512) null comment '缩略图URL';

-- 空间表
create table if not exists space
(
    id  bigint auto_increment comment 'id' primary key ,
    spaceName   varchar(128) null comment '空间名称',
    spaceLevel  int default 0  null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize bigint default 0 null comment '空间图片的最大总大小',
    maxCount bigint default 0 null comment '空间图片的最大数量',
    totalSize bigint default 0 null comment '当前空间下图片的总大小',
    totalCount bigint default 0 null comment '当前空间下的图片数量',
    userId bigint not null comment '创建用户 id',
    createTime datetime default current_timestamp not null comment '创建时间',
    editTime datetime default current_timestamp  not null comment '编辑时间',
    updateTime datetime default current_timestamp on update current_timestamp not null comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',
    -- 添加索引
    index idx_userId (userId), -- 提升基于用户ID的查询性能
    index idx_spaceName (spaceName), -- 提升基于空间名称的查询性能
    index idx_spaceLevel (spaceLevel) -- 提升基于空间级别的查询性能
)comment '空间' collate = utf8mb4_unicode_ci;

-- 图片表添加空间id列
alter table picture add column spaceId bigint null comment '空间id,（null）表示为公共空间';
-- 添加索引
create index idx_spaceId on picture (spaceId);

-- 添加色调字段
alter table picture add column picColor varchar(16) null comment '图片主色调';

ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';
CREATE INDEX idx_spaceType ON space (spaceType);

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;