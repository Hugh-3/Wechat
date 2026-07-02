const { get, put } = require('../utils/request');

/**
 * 获取当前用户统计数据
 */
function getMyStats() {
  return get('/v1/users/stats');
}

/**
 * 更新用户资料（昵称、头像）
 */
function updateProfile(data) {
  return put('/v1/users/profile', data);
}

/**
 * 获取获赞记录
 */
function getMyLikes(page, pageSize) {
  return get('/v1/users/likes', { page, pageSize });
}

/**
 * 获取评论记录
 */
function getMyComments(page, pageSize) {
  return get('/v1/users/comments', { page, pageSize });
}

module.exports = {
  getMyStats,
  updateProfile,
  getMyLikes,
  getMyComments
};
