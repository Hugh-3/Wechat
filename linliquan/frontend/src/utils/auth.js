/**
 * 用户认证状态管理工具
 * 前端使用，用于在不同认证状态间切换（开发调试用）
 */
export const UserStatus = {
  UNAUTH: 0,
  PENDING: 1,
  VERIFIED: 2
};

export const UserStatusDesc = {
  [UserStatus.UNAUTH]: '未认证',
  [UserStatus.PENDING]: '认证中',
  [UserStatus.VERIFIED]: '已认证'
};

// Mock用户数据工厂
export const createMockUser = (status) => ({
  id: 1,
  nickname: '测试用户',
  avatarUrl: 'https://example.com/avatar.png',
  verificationStatus: status,
  verificationDesc: UserStatusDesc[status]
});

// 当前用户状态（本地存储，开发时手动切换）
export const getCurrentUserStatus = () => {
  return wx.getStorageSync('mockUserStatus') || UserStatus.UNAUTH;
};

export const setCurrentUserStatus = (status) => {
  wx.setStorageSync('mockUserStatus', status);
};

// 权限检查工具函数
export const canWrite = (status) => status === UserStatus.VERIFIED;
export const canRead = (status) => true; // 三种状态均可读

// 获取认证状态描述
export const getStatusDesc = (status) => UserStatusDesc[status] || '未知';
