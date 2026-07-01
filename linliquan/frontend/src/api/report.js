const { get, post } = require('../utils/request');

/**
 * 创建举报
 * @param {number} targetType - 举报目标类型：1-帖子, 2-评论, 3-用户
 * @param {number} targetId - 目标ID
 * @param {number} reason - 举报原因：1-垃圾广告, 2-违法违规, 3-色情低俗, 4-侮辱谩骂, 5-其他
 * @param {string} description - 补充描述（可选）
 */
function createReport(targetType, targetId, reason, description) {
  return post('/v1/reports', { targetType, targetId, reason, description });
}

/**
 * 获取我的举报记录（分页）
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
function getMyReports(page, pageSize) {
  return get('/v1/reports/my', { page, pageSize });
}

module.exports = {
  createReport,
  getMyReports
};
