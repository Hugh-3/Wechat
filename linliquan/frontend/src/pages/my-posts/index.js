// pages/my-posts/index.js
// 我的发布页面，展示当前用户发布的帖子列表

const postApi = require('../../api/post');
const { formatRelativeTime } = require('../../utils/time');
const { getCurrentUserStatus } = require('../../utils/auth');

Page({
  data: {
    posts: [],            // 全部已加载的原始帖子数据
    displayPosts: [],    // 按 Tab 过滤后用于展示的帖子
    page: 1,              // 当前页码
    pageSize: 10,         // 每页数量
    hasMore: true,        // 是否还有更多
    loading: false,       // 是否正在加载
    activeTab: 'all',     // 当前选中的 Tab：all / info(1) / help(2)
    userStatus: 0        // 当前用户认证状态
  },

  onLoad() {
    // 检查用户状态
    const status = getCurrentUserStatus();
    this.setData({ userStatus: status });

    this.loadPosts();
  },

  onShow() {
    // 页面再次展示时同步最新的用户状态
    const status = getCurrentUserStatus();
    this.setData({ userStatus: status });
  },

  // 切换 Tab，仅在前端过滤已加载的数据
  onTabChange(e) {
    const type = e.currentTarget.dataset.type;
    if (type === this.data.activeTab) return;
    this.setData({ activeTab: type });
    this.updateDisplayPosts();
  },

  // 按 Tab 重新计算展示列表
  updateDisplayPosts() {
    const { posts, activeTab } = this.data;
    let displayPosts;
    if (activeTab === 'all') {
      displayPosts = posts;
    } else {
      // info 对应 postType=1，help 对应 postType=2
      const typeNum = activeTab === 'info' ? 1 : 2;
      displayPosts = posts.filter(p => p.postType === typeNum);
    }
    this.setData({ displayPosts });
  },

  // 加载更多帖子
  async loadPosts() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const res = await postApi.getMyPosts(this.data.page, this.data.pageSize);
      const result = res.data || { list: [], total: 0 };
      const list = (result.list || []).map(item => ({
        ...item,
        timeText: formatRelativeTime(item.createdAt)
      }));

      const newPosts = [...this.data.posts, ...list];
      this.setData({
        posts: newPosts,
        page: this.data.page + 1,
        hasMore: list.length >= this.data.pageSize
      });
      // 同步更新展示列表
      this.updateDisplayPosts();
    } catch (err) {
      console.error('加载我的发布失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 点击帖子跳转详情
  onPostTap(e) {
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}` });
  },

  // 上拉加载更多
  onReachBottom() {
    this.loadPosts();
  },

  // 下拉刷新
  async onPullDownRefresh() {
    this.setData({ posts: [], displayPosts: [], page: 1, hasMore: true });
    await this.loadPosts();
    wx.stopPullDownRefresh();
  }
});
