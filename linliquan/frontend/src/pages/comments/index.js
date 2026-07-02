// pages/comments/index.js
// 评论记录页面，展示我的评论记录

const userApi = require('../../api/user');
const { formatRelativeTime } = require('../../utils/time');

// 模拟评论记录数据（后端尚未提供单独的评论记录接口）
const MOCK_COMMENT_RECORDS = [
  {
    id: 1,
    postTitle: '小区南门新开的早餐店推荐',
    content: '去尝过了，味道确实不错，价格也实惠！',
    commentedAt: Date.now() - 1 * 60 * 60 * 1000
  },
  {
    id: 2,
    postTitle: '关于停车位分配的一点建议',
    content: '支持楼主的建议，希望物业能够采纳。',
    commentedAt: Date.now() - 5 * 60 * 60 * 1000
  },
  {
    id: 3,
    postTitle: '周末组织小区亲子活动',
    content: '报名+1，请问需要自带什么物品吗？',
    commentedAt: Date.now() - 1 * 24 * 60 * 60 * 1000
  },
  {
    id: 4,
    postTitle: '邻里互助：帮忙代收快递',
    content: '太感谢了，已经帮忙取回了，邻里之间就该互相帮助。',
    commentedAt: Date.now() - 3 * 24 * 60 * 60 * 1000
  },
  {
    id: 5,
    postTitle: '小区绿化建议征集',
    content: '建议在凉亭附近多种一些遮阴的树木。',
    commentedAt: Date.now() - 6 * 24 * 60 * 60 * 1000
  }
];

Page({
  data: {
    totalComments: 0,  // 总评论数
    records: [],       // 评论记录列表
    loading: false
  },

  onLoad() {
    this.loadStats();
    this.loadRecords();
  },

  // 下拉刷新
  onPullDownRefresh() {
    Promise.all([
      this.loadStats(),
      this.loadRecords()
    ]).finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 加载评论总数
  async loadStats() {
    try {
      const res = await userApi.getMyStats();
      const data = res.data || {};
      this.setData({ totalComments: data.commentCount || data.totalComments || 0 });
    } catch (err) {
      console.error('加载评论统计失败', err);
    }
  },

  // 加载评论记录（暂用模拟数据，后端接口就绪后替换）
  async loadRecords() {
    this.setData({ loading: true });
    try {
      // 模拟网络请求延迟
      await new Promise(resolve => setTimeout(resolve, 200));
      const records = MOCK_COMMENT_RECORDS.map(item => ({
        ...item,
        timeText: formatRelativeTime(item.commentedAt)
      }));
      this.setData({ records });
    } catch (err) {
      console.error('加载评论记录失败', err);
    } finally {
      this.setData({ loading: false });
    }
  }
});
