/**
 * 用户认证状态管理工具
 * 前端使用，用于在不同认证状态间切换（开发调试用）
 */

const UserStatus = {
  UNAUTH: 0,
  PENDING: 1,
  VERIFIED: 2
};

const UserStatusDesc = {
  [UserStatus.UNAUTH]: '未认证',
  [UserStatus.PENDING]: '认证中',
  [UserStatus.VERIFIED]: '已认证'
};

const statusConfig = {
  UNAUTH: {
    status: 0,
    label: '未认证',
    color: '#999',
    canWrite: false,
    canRead: true,
    publishButton: {
      enabled: false,
      badge: null,
      tooltip: null
    }
  },
  PENDING: {
    status: 1,
    label: '认证中',
    color: '#f5a623',
    canWrite: false,
    canRead: true,
    publishButton: {
      enabled: false,
      badge: '审核中',
      badgeColor: '#f5a623',
      tooltip: '您的业主认证正在审核中，预计1-2个工作日完成'
    }
  },
  VERIFIED: {
    status: 2,
    label: '已认证',
    color: '#07c160',
    canWrite: true,
    canRead: true,
    publishButton: {
      enabled: true,
      badge: null,
      tooltip: null
    }
  }
};

function createMockUser(status) {
  return {
    id: 1,
    nickname: '测试用户',
    avatarUrl: 'https://example.com/avatar.png',
    verificationStatus: status,
    verificationDesc: UserStatusDesc[status]
  };
}

function getCurrentUserStatus() {
  return wx.getStorageSync('mockUserStatus') || UserStatus.UNAUTH;
}

function setCurrentUserStatus(status) {
  wx.setStorageSync('mockUserStatus', status);
}

function canWrite(status) {
  if (status === undefined) {
    status = getCurrentUserStatus();
  }
  return status === UserStatus.VERIFIED;
}

function canRead(status) {
  return true;
}

function getStatusDesc(status) {
  return UserStatusDesc[status] || '未知';
}

function getStatusConfig(status) {
  if (status === 0) return statusConfig.UNAUTH;
  if (status === 1) return statusConfig.PENDING;
  return statusConfig.VERIFIED;
}

function getFuzzyDistance(distance) {
  const num = parseInt(distance);
  if (isNaN(num)) return distance;
  if (num < 300) return '约500米内';
  if (num < 800) return '约1公里内';
  if (num < 3000) return '约3公里内';
  return '约5公里内';
}

function formatDistance(distance) {
  if (!distance && distance !== 0) return '';
  const num = parseInt(distance);
  if (isNaN(num)) return distance;
  if (num < 1000) {
    return num + '米';
  }
  return (num / 1000).toFixed(1) + '公里';
}

module.exports = {
  UserStatus,
  UserStatusDesc,
  statusConfig,
  createMockUser,
  getCurrentUserStatus,
  setCurrentUserStatus,
  canWrite,
  canRead,
  getStatusDesc,
  getStatusConfig,
  getFuzzyDistance,
  formatDistance
};
