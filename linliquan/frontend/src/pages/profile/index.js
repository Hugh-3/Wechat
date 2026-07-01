// pages/profile/index.js
// 个人中心页面

const { getCurrentUserStatus, getStatusConfig } = require('../../utils/auth');

Page({
  data: {
    userInfo: {
      nickname: '',
      avatar: ''
    },
    userStatus: 0,
    userStatusDesc: '',
    isVerified: false,
    isPending: false,
    verificationText: '',
    verificationStatusClass: '',
    verificationIcon: '',
    stats: {
      postCount: 0,
      likeCount: 0,
      commentCount: 0
    }
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  // 加载用户信息
  loadUserInfo() {
    const status = getCurrentUserStatus();
    const config = getStatusConfig(status);

    let verificationText = '';
    let verificationStatusClass = '';
    let verificationIcon = '';

    if (status === 2) {
      verificationText = '已认证业主';
      verificationStatusClass = 'verified';
      verificationIcon = '✓';
    } else if (status === 1) {
      verificationText = '认证审核中';
      verificationStatusClass = 'pending';
      verificationIcon = '⏳';
    } else {
      verificationText = '未认证';
      verificationStatusClass = 'unauth';
      verificationIcon = '🔒';
    }

    this.setData({
      userStatus: status,
      userStatusDesc: config.label,
      isVerified: status === 2,
      isPending: status === 1,
      verificationText,
      verificationStatusClass,
      verificationIcon,
      userInfo: {
        nickname: '邻里用户',
        avatar: '/assets/default-avatar.png'
      },
      stats: {
        postCount: status === 2 ? 3 : 0,
        likeCount: status === 2 ? 28 : 0,
        commentCount: status === 2 ? 12 : 0
      }
    });
  },

  // 编辑资料
  onEditProfile() {
    wx.showToast({ title: '资料编辑功能开发中', icon: 'none' });
  },

  // 跳转认证页
  goToAuth() {
    wx.navigateTo({ url: '/pages/auth/index' });
  },

  // 我的发布
  goToMyPosts() {
    if (!this.data.isVerified) {
      wx.showToast({ title: '认证后可查看', icon: 'none' });
      return;
    }
    wx.showToast({ title: '我的发布功能开发中', icon: 'none' });
  },

  // 我的互助
  goToMyHelps() {
    if (!this.data.isVerified) {
      wx.showToast({ title: '认证后可查看', icon: 'none' });
      return;
    }
    wx.showToast({ title: '我的互助功能开发中', icon: 'none' });
  },

  // 我的收藏
  goToMyFavorites() {
    wx.showToast({ title: '收藏功能开发中', icon: 'none' });
  },

  // 获赞
  goToMyLikes() {
    wx.showToast({ title: '获赞记录功能开发中', icon: 'none' });
  },

  // 评论
  goToMyComments() {
    wx.showToast({ title: '评论记录功能开发中', icon: 'none' });
  },

  // 设置
  goToSettings() {
    wx.showToast({ title: '设置功能开发中', icon: 'none' });
  },

  // 帮助与反馈
  goToHelp() {
    wx.showToast({ title: '帮助功能开发中', icon: 'none' });
  },

  // 关于
  goToAbout() {
    wx.showModal({
      title: '关于邻里圈',
      content: '邻里圈 v1.0.0\n\n让邻里更亲近，让生活更美好\n\n© 2026 邻里圈团队',
      showCancel: false
    });
  },

  // 切换状态（调试用）
  switchStatus() {
    const statuses = [0, 1, 2];
    const current = this.data.userStatus;
    const nextIndex = (statuses.indexOf(current) + 1) % statuses.length;
    const nextStatus = statuses[nextIndex];
    wx.setStorageSync('mockUserStatus', nextStatus);
    this.loadUserInfo();
  }
});
