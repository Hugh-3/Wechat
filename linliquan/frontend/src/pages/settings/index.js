// pages/settings/index.js
// 设置页面：消息通知、缓存清理、环境切换、关于、退出登录

const env = require('../../utils/env');

// 本地存储中保存通知开关的 key
const NOTIFICATION_STORAGE_KEY = 'settings_notification';

Page({
  data: {
    notificationEnabled: true,
    cacheSize: '0 KB',
    currentEnv: 'mock',
    envList: [],
    version: 'v1.2.0'
  },

  onLoad() {
    // 读取本地存储的设置项
    this.loadSettings();
    // 计算当前缓存大小
    this.calcCacheSize();
    // 初始化环境列表与当前环境
    const currentEnv = env.getCurrentEnv();
    const envList = env.listEnvs();
    this.setData({
      currentEnv,
      envList
    });
  },

  onShow() {
    // 页面再次展示时刷新缓存大小
    this.calcCacheSize();
  },

  // 读取本地存储的设置项
  loadSettings() {
    let notificationEnabled = true;
    try {
      const saved = wx.getStorageSync(NOTIFICATION_STORAGE_KEY);
      // 已保存过则以保存的值为准
      if (saved === true || saved === false) {
        notificationEnabled = saved;
      }
    } catch (e) {}
    this.setData({ notificationEnabled });
  },

  // 计算当前缓存大小
  calcCacheSize() {
    try {
      const info = wx.getStorageInfoSync();
      // currentSize 单位为 KB
      const size = info.currentSize || 0;
      this.setData({
        cacheSize: this.formatCacheSize(size)
      });
    } catch (e) {}
  },

  // 格式化缓存大小展示
  formatCacheSize(kb) {
    if (kb < 1) {
      return '0 KB';
    }
    if (kb < 1024) {
      return kb + ' KB';
    }
    return (kb / 1024).toFixed(2) + ' MB';
  },

  // 消息通知开关切换
  onNotificationChange(e) {
    const value = e.detail.value;
    try {
      wx.setStorageSync(NOTIFICATION_STORAGE_KEY, value);
    } catch (e) {}
    this.setData({ notificationEnabled: value });
    wx.showToast({
      title: value ? '已开启通知' : '已关闭通知',
      icon: 'none'
    });
  },

  // 清理缓存
  onClearCache() {
    wx.showModal({
      title: '清理缓存',
      content: '确定要清理缓存吗？清理后不会影响您的登录状态。',
      confirmColor: '#07c160',
      success: (res) => {
        if (!res.confirm) return;
        // 清理前先备份需要保留的必要数据
        let keepToken = '';
        let keepEnv = '';
        let keepNotification = this.data.notificationEnabled;
        try {
          keepToken = wx.getStorageSync('accessToken') || '';
        } catch (e) {}
        try {
          keepEnv = env.getCurrentEnv();
        } catch (e) {}

        // 清空全部本地缓存
        try {
          wx.clearStorageSync();
        } catch (e) {}

        // 重新设置必要数据：token、环境、通知设置
        try {
          if (keepToken) {
            wx.setStorageSync('accessToken', keepToken);
          }
        } catch (e) {}
        try {
          if (keepEnv) {
            env.setCurrentEnv(keepEnv);
          }
        } catch (e) {}
        try {
          wx.setStorageSync(NOTIFICATION_STORAGE_KEY, keepNotification);
        } catch (e) {}

        // 重新计算缓存大小
        this.calcCacheSize();
        wx.showToast({
          title: '缓存已清理',
          icon: 'success'
        });
      }
    });
  },

  // 环境切换：弹出 ActionSheet 选择
  onSwitchEnv() {
    const envList = this.data.envList;
    if (!envList || envList.length === 0) {
      return;
    }
    const itemList = envList.map(item => {
      const tag = item.key === this.data.currentEnv ? '（当前）' : '';
      return item.name + tag;
    });
    wx.showActionSheet({
      itemList,
      success: (res) => {
        const selected = envList[res.tapIndex];
        if (!selected) return;
        if (selected.key === this.data.currentEnv) {
          return;
        }
        env.setCurrentEnv(selected.key);
        this.setData({ currentEnv: selected.key });
        wx.showToast({
          title: '已切换至' + selected.name,
          icon: 'none'
        });
      }
    });
  },

  // 关于邻里圈
  onAbout() {
    wx.showModal({
      title: '关于邻里圈',
      content: '邻里圈 ' + this.data.version + '\n\n让邻里更亲近，让生活更美好\n\n© 2026 邻里圈团队',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  // 退出登录
  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      confirmColor: '#fa5151',
      success: (res) => {
        if (!res.confirm) return;
        // 清除 token 和用户信息
        try {
          wx.removeStorageSync('accessToken');
          wx.removeStorageSync('userInfo');
        } catch (e) {}
        // 同步清除全局 token（如存在 app 实例）
        const app = getApp();
        if (app && app.globalData) {
          app.globalData.accessToken = '';
        }
        wx.showToast({
          title: '已退出登录',
          icon: 'success'
        });
        // 跳转到首页（tabBar 页面）
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index'
          });
        }, 500);
      }
    });
  }
});
