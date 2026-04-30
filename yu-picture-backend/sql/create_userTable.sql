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