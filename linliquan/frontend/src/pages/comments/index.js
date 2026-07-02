// pages/comments/index.js
// 评论记录页面，展示我的评论记录

const userApi = require('../../api/user');
const { formatRelativeTime } = require('../../utils/time');

Page({
  data: {
    totalComments: 0,  // 总评论数
    records: [],       // 评论记录列表
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

  // 加载评论总数
  async loadStats() {
    try {
      const res = await userApi.getMyStats();
      const data = res.data || {};
      this.setData({
        totalComments: data.commentCount != null ? data.commentCount : (data.totalComments || 0)
      });
    } catch (err) {
      console.error('加载评论统计失败', err);
    }
  },

  // 加载评论记录
  async loadRecords() {
    this.setData({ loading: true });
    try {
      const { page, pageSize } = this.data;
      const res = await userApi.getMyComments(page, pageSize);
      const data = res.data || {};
      const list = data.list || [];
      // 映射接口字段到 wxml 绑定字段
      const records = list.map(item => ({
        id: item.postId,
        postId: item.postId,
        postTitle: item.postTitle,
        content: item.content,
        timeText: formatRelativeTime(item.createdAt)
      }));
      this.setData({
        records: page === 1 ? records : this.data.records.concat(records),
        hasMore: list.length >= pageSize,
        totalComments: data.total != null ? data.total : this.data.totalComments
      });
    } catch (err) {
      console.error('加载评论记录失败', err);
    } finally {
      this.setData({ loading: false });
    }
  }
});
