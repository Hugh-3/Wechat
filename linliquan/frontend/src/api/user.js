const { get } = require('../utils/request');

/**
 * 获取当前用户统计数据
 */
function getMyStats() {
  return get('/v1/users/stats');
}

module.exports = {
  getMyStats
};
