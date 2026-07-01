/**
 * 时间格式化工具
 */

/**
 * 格式化相对时间
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 相对时间描述
 */
const formatRelativeTime = (time) => {
  if (!time) return '';

  const date = typeof time === 'string' ? new Date(time.replace(/-/g, '/')) : new Date(time);
  const now = new Date();
  const diff = now - date;

  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const week = 7 * day;
  const month = 30 * day;

  if (diff < minute) {
    return '刚刚';
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前';
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前';
  } else if (diff < week) {
    return Math.floor(diff / day) + '天前';
  } else if (diff < month) {
    return Math.floor(diff / week) + '周前';
  } else {
    const year = date.getFullYear();
    const monthNum = date.getMonth() + 1;
    const dayNum = date.getDate();
    if (year === now.getFullYear()) {
      return `${monthNum}月${dayNum}日`;
    }
    return `${year}-${monthNum}-${dayNum}`;
  }
};

/**
 * 格式化日期时间
 * @param {string|Date} time - 时间
 * @param {string} format - 格式
 * @returns {string}
 */
const formatDateTime = (time, format = 'YYYY-MM-DD HH:mm') => {
  if (!time) return '';

  const date = typeof time === 'string' ? new Date(time.replace(/-/g, '/')) : new Date(time);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
};

module.exports = {
  formatRelativeTime,
  formatDateTime
};
