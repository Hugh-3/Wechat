// pages/likes/index.js
// 获赞记录页面，展示收到的点赞记录

const userApi = require('../../api/user');
const { formatRelativeTime } = require('../../utils/time');

Page({
  data: {
    totalLikes: 0,    // 总获赞数
    records: [],      // 获赞记录列表
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false
  },

  onLoad() {
    this.loadStats();
    this.loadRecords();
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true });
    Promise.all([
      this.loadStats(),
      this.loadRecords()
    ]).finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 上拉加载更多
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 });
    this.loadRecords();
  },

  // 点击记录跳转帖子详情
  onRecordTap(e) {
    const postId = e.currentTarget.dataset.postId;
    if (!postId) return;
    wx.navigateTo({
      url: `/pages/post-detail/index?id=${postId}`
    });
  },

  // 加载获赞总数
  async loadStats() {
    try {
      const res = await userApi.getMyStats();
      const data = res.data || {};
      this.setData({
        totalLikes: data.likeCount != null ? data.likeCount : (data.totalLikes || 0)
      });
    } catch (err) {
      console.error('加载获赞统计失败', err);
    }
  },

  // 加载获赞记录
  async loadRecords() {
    this.setData({ loading: true });
    try {
      const { page, pageSize } = this.data;
      const res = await userApi.getMyLikes(page, pageSize);
      const data = res.data || {};
      const list = data.list || [];
      // 映射接口字段到 wxml 绑定字段
      const records = list.map(item => ({
        id: item.postId,
        postId: item.postId,
        likerNickname: item.likerName,
        likerAvatar: item.likerAvatar,
        postTitle: item.title,
        content: item.content,
        timeText: formatRelativeTime(item.createdAt)
      }));
      this.setData({
        records: page === 1 ? records : this.data.records.concat(records),
        hasMore: list.length >= pageSize,
        totalLikes: data.total != null ? data.total : this.data.totalLikes
      });
    } catch (err) {
      console.error('加载获赞记录失败', err);
    } finally {
      this.setData({ loading: false });
    }
  }
});
