// pages/index/index.js
// 信息广场页面

const { getCurrentUserStatus, getStatusConfig, canWrite } = require('../../utils/auth');
const postApi = require('../../api/post');
const { formatRelativeTime } = require('../../utils/time');

Page({
  data: {
    posts: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    userStatus: 0,
    userStatusDesc: '',
    publishEnabled: false,
    publishBadge: null,
    activeTab: 'all',
  },

  onLoad() {
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
    const status = getCurrentUserStatus();
    const config = getStatusConfig(status);
    this.setData({
      userStatus: status,
      publishEnabled: config.canWrite,
      publishBadge: config.publishButton?.badge || null
    });
  },

  onTabChange(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ activeTab: type, posts: [], page: 1, hasMore: true });
    this.loadPosts();
  },

  async loadPosts() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const type = this.data.activeTab === 'all' ? null :
                   this.data.activeTab === 'info' ? 1 : 2;
      const res = await postApi.getList(type, this.data.page, this.data.pageSize);
      const result = res.data || { list: [], total: 0 };

      const processedList = (result.list || []).map(item => ({
        ...item,
        timeText: formatRelativeTime(item.createdAt),
        liked: item.liked || false
      }));

      this.setData({
        posts: [...this.data.posts, ...processedList],
        page: this.data.page + 1,
        hasMore: (result.list || []).length >= this.data.pageSize
      });
    } catch (err) {
      console.error('加载失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onAuthBannerTap() {
    const { userStatus } = this.data;
    if (userStatus === 1) {
      wx.showToast({ title: '认证正在审核中，请耐心等待', icon: 'none' });
    } else {
      wx.navigateTo({ url: '/pages/auth/index' });
    }
  },

  onPublishTap() {
    const { userStatus, publishEnabled } = this.data;

    if (!publishEnabled) {
      if (userStatus === 1) {
        wx.showToast({
          title: '您的业主认证正在审核中',
          icon: 'none',
          duration: 2000
        });
      } else {
        this.showAuthGuideModal();
      }
      return;
    }

    wx.navigateTo({ url: '/pages/publish/index' });
  },

  showAuthGuideModal() {
    wx.showModal({
      title: '为了保护您和邻居的安全',
      content: '只有认证业主才能使用此功能\n\n🔒 您的证件信息将加密存储\n📋 审核通过后7天自动删除原图\n✅ 认证通过即可发布/互动',
      confirmText: '去认证',
      cancelText: '稍后再说',
      showCancel: true,
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/auth/index' });
        }
      }
    });
  },

  onPostTap(e) {
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}` });
  },

  onLikeTap(e) {
    if (!this.data.publishEnabled) {
      this.showAuthGuideModal();
      return;
    }
    const postId = e.currentTarget.dataset.id;
    const posts = [...this.data.posts];
    const index = posts.findIndex(p => p.id === postId);
    if (index === -1) return;

    const post = posts[index];
    const newLiked = !post.liked;
    const newLikeCount = newLiked ? post.likeCount + 1 : post.likeCount - 1;

    posts[index] = {
      ...post,
      liked: newLiked,
      likeCount: newLikeCount
    };

    this.setData({ posts });

    postApi.toggleLike(postId, newLiked).catch(() => {
      posts[index] = {
        ...post,
        liked: post.liked,
        likeCount: post.likeCount
      };
      this.setData({ posts });
      wx.showToast({ title: '操作失败', icon: 'none' });
    });
  },

  onCommentTap(e) {
    if (!this.data.publishEnabled) {
      this.showAuthGuideModal();
      return;
    }
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}&focus=comment` });
  },

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
