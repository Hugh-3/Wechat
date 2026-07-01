const { get, post } = require('../utils/request');

/**
 * 创建评价
 * @param {number} orderId - 订单ID
 * @param {number} rating - 评分 1-5
 * @param {string} content - 评价内容
 * @param {string[]} images - 评价图片URL数组
 */
function createReview(orderId, rating, content, images) {
  return post('/v1/reviews', { orderId, rating, content, images });
}

/**
 * 获取订单的所有评价
 * @param {number} orderId - 订单ID
 */
function getOrderReviews(orderId) {
  return get(`/v1/reviews/order/${orderId}`);
}

/**
 * 获取用户收到的评价（分页）
 * @param {number} userId - 用户ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
function getUserReviews(userId, page, pageSize) {
  return get(`/v1/reviews/user/${userId}`, { page, pageSize });
}

/**
 * 获取用户评分汇总（平均分、总数、1-5星分布）
 * @param {number} userId - 用户ID
 */
function getUserRatingSummary(userId) {
  return get(`/v1/reviews/user/${userId}/summary`);
}

/**
 * 检查当前用户是否可评价该订单
 * @param {number} orderId - 订单ID
 */
function canReview(orderId) {
  return get(`/v1/reviews/order/${orderId}/can-review`);
}

module.exports = {
  createReview,
  getOrderReviews,
  getUserReviews,
  getUserRatingSummary,
  canReview
};
