// pages/index/index.js
// 信息广场页面

const { getCurrentUserStatus, getStatusConfig, canWrite } = require('../../utils/auth');
const { getPostList, createPost } = require('../../mock/data');

Page({
  data: {
    posts: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    // 用户状态相关
    userStatus: 0,
    userStatusDesc: '',
    publishEnabled: false,
    publishBadge: null,
    // 筛选
    activeTab: 'all', // all | info | help
  },

  onLoad() {
    // 获取当前用户状态
    const status = getCurrentUserStatus();
    const config = getStatusConfig(status);
    this.setData({
      userStatus: status,
      userStatusDesc: config.label,
      publishEnabled: config.canWrite,
      publishBadge: config.publishButton?.badge || null
    });
    this.loadPosts();
  },

  onShow() {
    // 每次显示时刷新状态（用于从认证页返回）
    const status = getCurrentUserStatus();
    const config = getStatusConfig(status);
    this.setData({
      userStatus: status,
      publishEnabled: config.canWrite,
      publishBadge: config.publishButton?.badge || null
    });
  },

  // 切换Tab
  onTabChange(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ activeTab: type, posts: [], page: 1, hasMore: true });
    this.loadPosts();
  },

  // 加载帖子列表
  async loadPosts() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const type = this.data.activeTab === 'all' ? null :
                   this.data.activeTab === 'info' ? 1 : 2;
      const result = await getPostList(type, this.data.page, this.data.pageSize);

      this.setData({
        posts: [...this.data.posts, ...result.list],
        page: this.data.page + 1,
        hasMore: result.list.length >= this.data.pageSize
      });
    } catch (err) {
      console.error('加载失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 点击发布按钮
  onPublishTap() {
    const { userStatus, publishEnabled } = this.data;

    if (!publishEnabled) {
      if (userStatus === 1) {
        // PENDING状态：显示Toast
        wx.showToast({
          title: '您的业主认证正在审核中',
          icon: 'none',
          duration: 2000
        });
      } else {
        // UNAUTH状态：弹出全屏认证引导
        this.showAuthGuideModal();
      }
      return;
    }

    // VERIFIED状态：打开发布页
    wx.navigateTo({ url: '/pages/publish/index' });
  },

  // 【红线强制】UNAUTH状态全屏认证引导弹窗（无关闭按钮）
  showAuthGuideModal() {
    wx.showModal({
      title: '为了保护您和邻居的安全',
      content: '只有认证业主才能使用此功能\n\n🔒 您的证件信息将加密存储\n📋 审核通过后7天自动删除原图\n✅ 认证通过即可发布/互动',
      confirmText: '去认证',
      cancelText: '稍后再说', // 底部灰色小字
      showCancel: true,
      success: (res) => {
        if (res.confirm) {
          // 跳转认证页
          wx.navigateTo({ url: '/pages/auth/index' });
        }
        // 无else，用户只能选择认证或稍后再说
      }
    });
  },

  // 点击帖子
  onPostTap(e) {
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}` });
  },

  // 点赞
  onLikeTap(e) {
    if (!this.data.publishEnabled) {
      this.showAuthGuideModal();
      return;
    }
    // 点赞逻辑
    const postId = e.currentTarget.dataset.id;
    console.log('点赞', postId);
  },

  // 评论
  onCommentTap(e) {
    if (!this.data.publishEnabled) {
      this.showAuthGuideModal();
      return;
    }
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}&focus=comment` });
  },

  // 切换用户状态（开发调试用）
  switchStatus() {
    const statuses = [0, 1, 2];
    const current = this.data.userStatus;
    const nextIndex = (statuses.indexOf(current) + 1) % statuses.length;
    const nextStatus = statuses[nextIndex];
    wx.setStorageSync('mockUserStatus', nextStatus);
    this.onLoad();
  },

  onReachBottom() {
    this.loadPosts();
  },

  onPullDownRefresh() {
    this.setData({ posts: [], page: 1, hasMore: true });
    this.loadPosts();
    wx.stopPullDownRefresh();
  }
});
