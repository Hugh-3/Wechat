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
-- comments表（评论表）
-- ==========================================
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    reply_to_user_id BIGINT REFERENCES users(id),
    content TEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,               -- 1-正常, 2-已删除
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post_id ON comments(post_id, created_at DESC);
CREATE INDEX idx_comments_user_id ON comments(user_id, created_at DESC);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);

-- ==========================================
-- post_likes表（帖子点赞记录表）
-- ==========================================
CREATE TABLE post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX idx_post_likes_user_id ON post_likes(user_id);

-- ==========================================
-- comment_likes表（评论点赞记录表）
-- ==========================================
CREATE TABLE comment_likes (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(comment_id, user_id)
);

CREATE INDEX idx_comment_likes_comment_id ON comment_likes(comment_id);
CREATE INDEX idx_comment_likes_user_id ON comment_likes(user_id);

-- ==========================================
-- favorites表（帖子收藏记录表）
-- ==========================================
CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE INDEX idx_favorites_post_id ON favorites(post_id);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);

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

-- ==========================================
-- admin_users表（管理员账户）
-- ==========================================
CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,              -- BCrypt加密
    nickname VARCHAR(50) NOT NULL DEFAULT '',
    role SMALLINT NOT NULL DEFAULT 1,                  -- 1-普通管理员, 2-超级管理员
    status SMALLINT NOT NULL DEFAULT 1,                 -- 1-正常, 2-禁用
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_users_username ON admin_users(username);

-- 为posts表增加审核相关字段
ALTER TABLE posts ADD COLUMN IF NOT EXISTS audit_status SMALLINT NOT NULL DEFAULT 0;  -- 0-待审核, 1-审核通过, 2-审核拒绝, 3-管理员下架
ALTER TABLE posts ADD COLUMN IF NOT EXISTS audit_reason VARCHAR(255);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS audited_by BIGINT REFERENCES admin_users(id);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS audited_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_posts_audit_status ON posts(audit_status);

-- 为comments表增加审核相关字段
ALTER TABLE comments ADD COLUMN IF NOT EXISTS audit_status SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS audit_reason VARCHAR(255);
ALTER TABLE comments ADD COLUMN IF NOT EXISTS audited_by BIGINT REFERENCES admin_users(id);
ALTER TABLE comments ADD COLUMN IF NOT EXISTS audited_at TIMESTAMP;

-- 为users表增加封禁字段
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_status SMALLINT NOT NULL DEFAULT 0;  -- 0-正常, 1-已封禁
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_reason VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_by BIGINT REFERENCES admin_users(id);
CREATE INDEX IF NOT EXISTS idx_users_ban_status ON users(ban_status);

-- ==========================================
-- notifications表（消息通知）
-- ==========================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),       -- 接收者
    type SMALLINT NOT NULL,                              -- 1-系统消息, 2-评论提醒, 3-接单通知, 4-点赞通知, 5-认证通知
    title VARCHAR(100) NOT NULL,
    content TEXT,
    related_type VARCHAR(50),                            -- 关联类型: post/comment/order/user
    related_id BIGINT,                                  -- 关联ID
    is_read SMALLINT NOT NULL DEFAULT 0,                -- 0-未读, 1-已读
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

-- ==========================================
-- points表（用户积分账户）
-- ==========================================
CREATE TABLE user_points (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    total_points INT NOT NULL DEFAULT 0,                -- 当前可用积分
    total_earned INT NOT NULL DEFAULT 0,                -- 累计获取积分
    total_consumed INT NOT NULL DEFAULT 0,              -- 累计消耗积分
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_points_user ON user_points(user_id);

-- ==========================================
-- point_transactions表（积分流水）
-- ==========================================
CREATE TABLE point_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type SMALLINT NOT NULL,                              -- 1-获取, 2-消耗
    points INT NOT NULL,                                 -- 积分变动值（正数）
    action VARCHAR(50) NOT NULL,                        -- post_create/order_accept/order_complete/like_received/exchange
    related_type VARCHAR(50),
    related_id BIGINT,
    remark VARCHAR(255),
    balance_after INT NOT NULL,                          -- 变动后余额
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_point_tx_user_created ON point_transactions(user_id, created_at DESC);
CREATE INDEX idx_point_tx_action ON point_transactions(action);

-- ==========================================
-- point_exchange_items表（积分兑换商品）
-- ==========================================
CREATE TABLE point_exchange_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    points_required INT NOT NULL,                        -- 所需积分
    stock INT NOT NULL DEFAULT 0,                        -- 库存
    status SMALLINT NOT NULL DEFAULT 1,                 -- 1-上架, 2-下架
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ==========================================
-- point_exchange_records表（兑换记录）
-- ==========================================
CREATE TABLE point_exchange_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    item_id BIGINT NOT NULL REFERENCES point_exchange_items(id),
    points_cost INT NOT NULL,                           -- 消耗积分
    status SMALLINT NOT NULL DEFAULT 1,                 -- 1-待发放, 2-已发放, 3-已取消
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exchange_records_user ON point_exchange_records(user_id, created_at DESC);

-- ==========================================
-- reviews表（互助评价）
-- ==========================================
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    reviewer_id BIGINT NOT NULL REFERENCES users(id),   -- 评价人
    reviewee_id BIGINT NOT NULL REFERENCES users(id),   -- 被评价人
    role SMALLINT NOT NULL,                              -- 1-发布者评价帮助者, 2-帮助者评价发布者
    rating SMALLINT NOT NULL,                            -- 1-5星
    content TEXT,
    images TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(order_id, reviewer_id)                       -- 每人每单只能评价一次
);

CREATE INDEX idx_reviews_order ON reviews(order_id);
CREATE INDEX idx_reviews_reviewee ON reviews(reviewee_id, created_at DESC);

-- ==========================================
-- reports表（用户举报）
-- ==========================================
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),   -- 举报人
    target_type SMALLINT NOT NULL,                       -- 1-帖子, 2-评论, 3-用户
    target_id BIGINT NOT NULL,
    reason SMALLINT NOT NULL,                            -- 1-垃圾广告, 2-违法违规, 3-色情低俗, 4-侮辱谩骂, 5-其他
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 0,                  -- 0-待处理, 1-已处理(有效), 2-已处理(无效)
    handle_remark VARCHAR(255),                          -- 处理备注
    handled_by BIGINT REFERENCES admin_users(id),
    handled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_status ON reports(status, created_at DESC);
CREATE INDEX idx_reports_reporter ON reports(reporter_id, created_at DESC);

-- 为已有触发器添加新表
CREATE TRIGGER update_admin_users_updated_at
    BEFORE UPDATE ON admin_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_points_updated_at
    BEFORE UPDATE ON user_points
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_point_exchange_items_updated_at
    BEFORE UPDATE ON point_exchange_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

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

-- 测试评论
INSERT INTO comments (post_id, user_id, content, like_count, status, created_at, updated_at) VALUES
(1, 4, '欢迎欢迎！期待更多邻居加入~', 5, 1, NOW(), NOW()),
(1, 3, '这个平台真不错，方便邻里交流', 3, 1, NOW(), NOW()),
(2, 4, '我可以帮忙，怎么联系你？', 2, 1, NOW(), NOW());

-- 测试管理员
INSERT INTO admin_users (username, password_hash, nickname, role, status, created_at, updated_at) VALUES
('admin', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7w5p5e8B9pKmJpJxKx1xKx1xKx1xK', '超级管理员', 2, 1, NOW(), NOW()),
('moderator', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7w5p5e8B9pKmJpJxKx1xKx1xKx1xK', '内容审核员', 1, 1, NOW(), NOW());

-- 测试通知
INSERT INTO notifications (user_id, type, title, content, related_type, related_id, is_read, created_at) VALUES
(3, 1, '欢迎来到邻里圈', '感谢您加入邻里圈社区互助平台', null, null, 0, NOW()),
(3, 2, '收到新评论', '已认证业主B评论了您的帖子', 'post', 2, 0, NOW()),
(3, 3, '您的互助任务被接单', '已认证业主B接了您的"帮忙取快递"任务', 'order', 1, 1, NOW());

-- 测试积分账户
INSERT INTO user_points (user_id, total_points, total_earned, total_consumed, updated_at) VALUES
(3, 120, 150, 30, NOW()),
(4, 85, 100, 15, NOW());

-- 测试积分流水
INSERT INTO point_transactions (user_id, type, points, action, remark, balance_after, created_at) VALUES
(3, 1, 50, 'post_create', '发布动态奖励', 50, NOW()),
(3, 1, 30, 'order_complete', '互助任务完成奖励', 80, NOW()),
(3, 1, 40, 'like_received', '获得点赞奖励', 120, NOW()),
(3, 2, 30, 'exchange', '兑换商品消耗', 90, NOW()),
(4, 1, 50, 'post_create', '发布动态奖励', 50, NOW()),
(4, 1, 35, 'order_accept', '接单奖励', 85, NOW());

-- 测试积分兑换商品
INSERT INTO point_exchange_items (name, description, image_url, points_required, stock, status, created_at, updated_at) VALUES
('社区定制帆布袋', '邻里圈定制款帆布购物袋', 'https://example.com/item1.png', 100, 50, 1, NOW(), NOW()),
('超市代金券20元', '社区合作超市代金券', 'https://example.com/item2.png', 200, 30, 1, NOW(), NOW()),
('社区咖啡券', '社区咖啡馆单杯兑换券', 'https://example.com/item3.png', 150, 10, 1, NOW(), NOW()),
('已下架测试商品', '用于测试下架逻辑', 'https://example.com/item4.png', 300, 0, 2, NOW(), NOW());

-- 测试评价
INSERT INTO reviews (order_id, reviewer_id, reviewee_id, role, rating, content, created_at) VALUES
(1, 4, 3, 2, 5, '发布者很友善，任务描述清晰，推荐合作！', NOW());

-- 测试举报
INSERT INTO reports (reporter_id, target_type, target_id, reason, description, status, created_at) VALUES
(4, 1, 3, 1, '疑似广告推广内容', 0, NOW()),
(3, 2, 1, 4, '评论内容含侮辱性语言', 0, NOW());

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

CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
