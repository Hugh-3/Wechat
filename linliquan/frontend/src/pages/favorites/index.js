// pages/favorites/index.js
// 我的收藏页面，展示当前用户收藏的帖子列表

const favoriteApi = require('../../api/favorite');
const { formatRelativeTime } = require('../../utils/time');
const { getCurrentUserStatus } = require('../../utils/auth');

// 删除按钮宽度（rpx），需与 wxss 中保持一致
const DELETE_BTN_WIDTH = 160;
// 触发滑动的最小水平位移
const SWIPE_THRESHOLD = 40;

Page({
  data: {
    posts: [],          // 已加载的收藏列表
    page: 1,            // 当前页码
    pageSize: 10,       // 每页数量
    hasMore: true,      // 是否还有更多
    loading: false,     // 是否正在加载
    userStatus: 0,      // 当前用户认证状态
    openIndex: -1       // 当前展开删除按钮的索引，-1 表示无
  },

  onLoad() {
    // 检查用户状态
    const status = getCurrentUserStatus();
    this.setData({ userStatus: status });

    this.loadFavorites();
  },

  onShow() {
    // 页面再次展示时同步最新的用户状态
    const status = getCurrentUserStatus();
    this.setData({ userStatus: status });
  },

  // 加载收藏列表
  async loadFavorites() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const res = await favoriteApi.getMyFavorites(this.data.page, this.data.pageSize);
      const result = res.data || { list: [], total: 0 };
      const list = (result.list || []).map(item => ({
        ...item,
        // 优先使用收藏时间，回退到创建时间
        timeText: formatRelativeTime(item.favoritedAt || item.createdAt),
        slideOffset: 0
      }));

      this.setData({
        posts: [...this.data.posts, ...list],
        page: this.data.page + 1,
        hasMore: list.length >= this.data.pageSize,
        openIndex: -1
      });
    } catch (err) {
      console.error('加载收藏失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 点击帖子跳转详情
  onPostTap(e) {
    // 若有滑出的删除按钮，先收起，不进行跳转
    if (this.data.openIndex !== -1) {
      this.closeAllSlide();
      return;
    }
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}` });
  },

  // 触摸开始
  onTouchStart(e) {
    this._startX = e.touches[0].clientX;
    this._startIndex = e.currentTarget.dataset.index;
  },

  // 触摸移动
  onTouchMove(e) {
    this._moveX = e.touches[0].clientX;
  },

  // 触摸结束：判断是否展开/收起删除按钮
  onTouchEnd() {
    const start = this._startX;
    const move = this._moveX;
    const index = this._startIndex;
    // 重置临时变量
    this._startX = undefined;
    this._moveX = undefined;
    this._startIndex = undefined;

    if (start === undefined || move === undefined) return;

    const delta = move - start;
    if (Math.abs(delta) < SWIPE_THRESHOLD) return;

    const posts = this.data.posts;
    if (!posts[index]) return;

    const isOpen = this.data.openIndex === index;
    // 左滑展开
    if (delta < 0 && !isOpen) {
      posts.forEach((p, i) => {
        p.slideOffset = i === index ? -DELETE_BTN_WIDTH : 0;
      });
      this.setData({ posts, openIndex: index });
    } else if (delta > 0 && isOpen) {
      // 右滑收起
      this.closeAllSlide();
    }
  },

  // 收起所有滑出的删除按钮
  closeAllSlide() {
    const posts = this.data.posts.map(p => ({ ...p, slideOffset: 0 }));
    this.setData({ posts, openIndex: -1 });
  },

  // 点击删除按钮：确认后取消收藏
  onDelete(e) {
    const index = e.currentTarget.dataset.index;
    const post = this.data.posts[index];
    if (!post) return;
    const postId = post.postId || post.id;

    wx.showModal({
      title: '取消收藏',
      content: '确定要取消收藏这条内容吗？',
      confirmText: '取消收藏',
      cancelText: '再想想',
      confirmColor: '#fa5151',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await favoriteApi.toggle(postId, false);
          // 从列表中移除
          const posts = this.data.posts.filter((_, i) => i !== index);
          this.setData({ posts, openIndex: -1 });
          wx.showToast({ title: '已取消收藏', icon: 'success' });
        } catch (err) {
          console.error('取消收藏失败', err);
          wx.showToast({ title: '操作失败', icon: 'none' });
        }
      }
    });
  },

  // 上拉加载更多
  onReachBottom() {
    this.loadFavorites();
  },

  // 下拉刷新
  async onPullDownRefresh() {
    this.setData({ posts: [], page: 1, hasMore: true, openIndex: -1 });
    await this.loadFavorites();
    wx.stopPullDownRefresh();
  }
});
