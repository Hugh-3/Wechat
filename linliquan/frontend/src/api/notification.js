const { get, post } = require('../utils/request');

/**
 * 获取通知列表（分页）
 */
function getNotifications(page, pageSize) {
  return get('/v1/notifications', { page, pageSize });
}

/**
 * 获取未读通知数量
 */
function getUnreadCount() {
  return get('/v1/notifications/unread-count');
}

/**
 * 标记单条通知为已读
 */
function markAsRead(notificationId) {
  return post(`/v1/notifications/${notificationId}/read`);
}

/**
 * 标记全部通知为已读
 */
function markAllAsRead() {
  return post('/v1/notifications/read-all');
}

module.exports = {
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead
};
