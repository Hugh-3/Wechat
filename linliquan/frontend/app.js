const envConfig = require('./src/utils/env');
const authApi = require('./src/api/auth');

App({
  onLaunch() {
    const env = envConfig.getCurrentEnv();
    this.globalData.currentEnv = env;

    if (envConfig.isMockMode()) {
      const mockStatus = wx.getStorageSync('mockUserStatus');
      if (mockStatus === '') {
        wx.setStorageSync('mockUserStatus', 0);
      }
    }

    this.checkTokenAndStatus();
  },

  globalData: {
    userInfo: null,
    accessToken: '',
    currentEnv: 'mock'
  },

  checkTokenAndStatus() {
    const token = wx.getStorageSync('accessToken');
    const userInfo = wx.getStorageSync('userInfo');

    if (token) {
      this.globalData.accessToken = token;
    }
    if (userInfo) {
      this.globalData.userInfo = userInfo;
    }

    if (token) {
      authApi.getStatus().then((res) => {
        if (res && res.success && res.data) {
          const statusData = res.data;
          const currentUserInfo = this.globalData.userInfo || {};
          currentUserInfo.verificationStatus = statusData.verificationStatus;
          currentUserInfo.verificationDesc = statusData.description;
          this.globalData.userInfo = currentUserInfo;
          wx.setStorageSync('userInfo', currentUserInfo);
        }
      }).catch(() => {
      });
    }
  },

  checkAuth() {
    const token = wx.getStorageSync('accessToken');
    const userInfo = wx.getStorageSync('userInfo');
    return !!(token && userInfo);
  },

  showAuthModal(options = {}) {
    const { title = '需要业主认证', content = '认证业主后才能使用此功能\n\n🔒 安全认证，保护邻里', confirmText = '去认证', cancelText = '稍后再说' } = options;

    wx.showModal({
      title,
      content,
      confirmText,
      cancelText,
      showCancel: true,
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/auth/index' });
        }
      }
    });
  },

  setUserInfo(userInfo) {
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('userInfo', userInfo);
  },

  setAccessToken(token) {
    this.globalData.accessToken = token;
    wx.setStorageSync('accessToken', token);
  },

  clearUserInfo() {
    this.globalData.userInfo = null;
    this.globalData.accessToken = '';
    wx.removeStorageSync('accessToken');
    wx.removeStorageSync('userInfo');
  }
});
