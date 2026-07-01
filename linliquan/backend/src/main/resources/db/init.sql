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

-- ==========================================
-- 测试数据（开发环境使用）
-- ==========================================

-- 测试用户
INSERT INTO users (phone_hash, phone_encrypted, nickname, avatar_url, verification_status, created_at, updated_at) VALUES
('a665a45920422f9d417e4867efdc4fb8', 'encrypted_phone_1', '未认证用户', 'https://example.com/avatar1.png', 0, NOW(), NOW()),
('a665a45920422f9d417e4867efdc4fb9', 'encrypted_phone_2', '认证中用户', 'https://example.com/avatar2.png', 1, NOW(), NOW()),
('a665a45920422f9d417e4867efdc4fb0', 'encrypted_phone_3', '已认证业主A', 'https://example.com/avatar3.png', 2, NOW(), NOW()),
('a665a45920422f9d417e4867efdc4fb1', 'encrypted_phone_4', '已认证业主B', 'https://example.com/avatar4.png', 2, NOW(), NOW());

-- 测试帖子
INSERT INTO posts (user_id, post_type, title, content, location, like_count, comment_count, view_count, status, created_at, updated_at) VALUES
(3, 1, '欢迎来到邻里圈', '这是我们社区的信息广场，欢迎大家分享生活点滴！', ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326)::geography, 10, 5, 100, 1, NOW(), NOW()),
(3, 2, '寻求帮助：帮忙取快递', '有谁能帮忙取个快递吗？酬金10元', ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326)::geography, 5, 2, 50, 1, NOW(), NOW()),
(4, 1, '周末跳蚤市场', '本周六小区广场有跳蚤市场，欢迎参加', ST_SetSRID(ST_MakePoint(116.4080, 39.9050), 4326)::geography, 20, 8, 200, 1, NOW(), NOW()),
(4, 2, '拼单买新鲜水果', '超市水果打折，附近的一起来拼单吧', ST_SetSRID(ST_MakePoint(116.4060, 39.9040), 4326)::geography, 3, 1, 30, 1, NOW(), NOW());

-- 测试互助任务
INSERT INTO orders (post_id, user_id, help_type, reward_amount, location, status, created_at, updated_at) VALUES
(2, 3, 2, 10.00, ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326)::geography, 1, NOW(), NOW()),
(4, 4, 1, 0.00, ST_SetSRID(ST_MakePoint(116.4060, 39.9040), 4326)::geography, 1, NOW(), NOW());

-- ==========================================
-- 视图定义
-- ==========================================

-- 帖子列表视图（关联用户信息）
CREATE OR REPLACE VIEW v_posts_with_user AS
SELECT
    p.id,
    p.user_id,
    p.post_type,
    p.title,
    p.content,
    p.like_count,
    p.comment_count,
    p.view_count,
    p.status,
    p.created_at,
    u.nickname AS user_name,
    u.avatar_url AS user_avatar,
    u.verification_status AS user_verification_status
FROM posts p
INNER JOIN users u ON p.user_id = u.id;

-- 附近互助任务视图
CREATE OR REPLACE VIEW v_nearby_orders AS
SELECT
    o.id,
    o.post_id,
    o.user_id,
    o.helper_user_id,
    o.help_type,
    o.reward_amount,
    o.status,
    o.created_at,
    p.title,
    p.content,
    u.nickname AS user_name,
    u.avatar_url AS user_avatar,
    h.nickname AS helper_name,
    ST_Distance(o.location, ST_SetSRID(ST_MakePoint(116.4074, 39.9042), 4326)::geography) AS distance_meters
FROM orders o
INNER JOIN posts p ON o.post_id = p.id
INNER JOIN users u ON o.user_id = u.id
LEFT JOIN users h ON o.helper_user_id = h.id;

-- ==========================================
-- 存储过程
-- ==========================================

-- 更新帖子点赞数
CREATE OR REPLACE FUNCTION update_post_like_count(post_id BIGINT, increment_val INT DEFAULT 1)
RETURNS VOID AS $$
BEGIN
    UPDATE posts
    SET like_count = like_count + increment_val,
        updated_at = NOW()
    WHERE id = post_id;
END;
$$ LANGUAGE plpgsql;

-- 获取用户认证统计
CREATE OR REPLACE FUNCTION get_verification_stats()
RETURNS TABLE(
    unauth_count BIGINT,
    pending_count BIGINT,
    verified_count BIGINT,
    total_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*) FILTER (WHERE verification_status = 0) AS unauth_count,
        COUNT(*) FILTER (WHERE verification_status = 1) AS pending_count,
        COUNT(*) FILTER (WHERE verification_status = 2) AS verified_count,
        COUNT(*) AS total_count
    FROM users;
END;
$$ LANGUAGE plpgsql;

-- ==========================================
-- 触发器
-- ==========================================

-- 自动更新updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
