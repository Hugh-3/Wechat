App({
  onLaunch() {
    const status = wx.getStorageSync('mockUserStatus');
    if (status === '') {
      wx.setStorageSync('mockUserStatus', 0);
    }
  },

  globalData: {
    userInfo: null,
    userStatus: 0
  },

  getCurrentUserStatus() {
    return wx.getStorageSync('mockUserStatus') || 0;
  },

  setCurrentUserStatus(status) {
    wx.setStorageSync('mockUserStatus', status);
    this.globalData.userStatus = status;
  }
});
