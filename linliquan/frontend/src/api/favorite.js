const { get, post } = require('../utils/request');

/**
 * 切换收藏状态
 */
function toggle(postId, favorited) {
  return post(`/v1/favorites/${postId}`, { favorited });
}

/**
 * 获取我的收藏列表
 */
function getMyFavorites(page, pageSize) {
  return get('/v1/favorites/my', { page, pageSize });
}

module.exports = {
  toggle,
  getMyFavorites
};
