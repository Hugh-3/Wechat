-- ==========================================
-- 【红线强制】邻里圈数据库初始化脚本
-- PostgreSQL + PostGIS
-- ==========================================

-- 启用PostGIS扩展
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- users表（用户表）
-- ==========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone_hash VARCHAR(64) UNIQUE NOT NULL,           -- 手机号SHA-256哈希（登录用）
    phone_encrypted VARCHAR(255) NOT NULL,             -- 手机号AES-256加密存储
    nickname VARCHAR(50) NOT NULL DEFAULT '',
    avatar_url VARCHAR(255),
    -- 【红线强制】认证状态枚举：0-UNAUTH, 1-PENDING, 2-VERIFIED
    verification_status SMALLINT NOT NULL DEFAULT 0,
    verification_apply_time TIMESTAMP,                 -- 认证申请时间
    verification_pass_time TIMESTAMP,                  -- 认证通过时间
    id_card_encrypted VARCHAR(255),                    -- 身份证AES-256加密
    house_number_encrypted VARCHAR(255),               -- 房号AES-256加密
    certificate_url VARCHAR(255),                     -- 认证材料COS路径
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 【红线强制】verification_status独立索引（供权限拦截中间件高频查询）
CREATE INDEX idx_users_verification_status ON users(verification_status);
CREATE INDEX idx_users_phone_hash ON users(phone_hash);

-- ==========================================
-- posts表（动态表 - 信息广场 + 邻里互助）
-- ==========================================
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    post_type SMALLINT NOT NULL,                       -- 1-信息广场, 2-邻里互助
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    images TEXT[],                                     -- PostgreSQL数组类型
    -- 【核心】PostGIS地理坐标点（SRID=4326为WGS84坐标系）
    location GEOGRAPHY(POINT, 4326),
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,               -- 1-正常, 2-已删除, 3-被屏蔽
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 【强制】空间GIST索引（查询附近5公里动态）
CREATE INDEX idx_posts_location_gist ON posts USING GIST(location);

-- 【强制】组合索引：类型+创建时间（信息广场常用查询）
CREATE INDEX idx_posts_type_created ON posts(post_type, created_at DESC);

-- B树索引
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

-- 仅对互助类型建局部索引（缩小索引体积）
CREATE INDEX idx_posts_help_location_gist ON posts USING GIST(location) WHERE post_type = 2;

-- ==========================================
-- orders表（互助任务表）
-- ==========================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    helper_user_id BIGINT REFERENCES users(id),
    help_type SMALLINT NOT NULL,                       -- 1-拼单团购, 2-代取代买, 3-生活求助, 4-技能交换
    reward_amount DECIMAL(10,2) DEFAULT 0,
    -- 【核心】互助任务位置坐标
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,                 -- 1-待接单, 2-进行中, 3-已完成, 4-已取消
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 【强制】空间GIST索引（查询附近5公里互助任务）
CREATE INDEX idx_orders_location_gist ON orders USING GIST(location);
CREATE INDEX idx_orders_status ON orders(status);

-- ==========================================
-- operation_logs表（等保要求：操作日志留存180天）
-- ==========================================
CREATE TABLE operation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    source_ip VARCHAR(45) NOT NULL,                    -- 支持IPv6
    action VARCHAR(50) NOT NULL,                        -- create_post/comment/login/verify等
    target_type VARCHAR(50),
    target_id BIGINT,
    detail JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 分表索引（按月分表，这里是模板）
CREATE INDEX idx_op_logs_user_created ON operation_logs(user_id, created_at DESC);
CREATE INDEX idx_op_logs_action_created ON operation_logs(action, created_at DESC);

-- ==========================================
-- auth_fail_logs表（等保要求：登录失败日志）
-- ==========================================
CREATE TABLE auth_fail_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    source_ip VARCHAR(45) NOT NULL,
    fail_reason VARCHAR(100),
    fail_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_fail_ip_time ON auth_fail_logs(source_ip, created_at DESC);
CREATE INDEX idx_auth_fail_user_time ON auth_fail_logs(user_id, created_at DESC);

-- ==========================================
-- 定时任务：每月创建新分表（operation_logs_YYYYMM）
-- ==========================================
-- 示例：CREATE TABLE operation_logs_202607 (LIKE operation_logs INCLUDING ALL);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.verification_status IS '认证状态：0-未认证(UNAUTH), 1-认证中(PENDING), 2-已认证(VERIFIED)';
COMMENT ON COLUMN posts.location IS 'PostGIS地理坐标点，SRID=4326';
COMMENT ON COLUMN orders.location IS '互助任务位置坐标，SRID=4326';
