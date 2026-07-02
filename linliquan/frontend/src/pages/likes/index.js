// pages/likes/index.js
// 获赞记录页面，展示收到的点赞记录

const userApi = require('../../api/user');
const { formatRelativeTime } = require('../../utils/time');

// 模拟获赞记录数据（后端尚未提供单独的获赞记录接口）
const MOCK_LIKE_RECORDS = [
  {
    id: 1,
    postTitle: '小区南门新开的早餐店推荐',
    likerNickname: '阳光邻居',
    likerAvatar: '/assets/default-avatar.png',
    likedAt: Date.now() - 30 * 60 * 1000
  },
  {
    id: 2,
    postTitle: '关于停车位分配的一点建议',
    likerNickname: '热心业主',
    likerAvatar: '/assets/default-avatar.png',
    likedAt: Date.now() - 3 * 60 * 60 * 1000
  },
  {
    id: 3,
    postTitle: '周末组织小区亲子活动',
    likerNickname: '快乐爸爸',
    likerAvatar: '/assets/default-avatar.png',
    likedAt: Date.now() - 1 * 24 * 60 * 60 * 1000
  },
  {
    id: 4,
    postTitle: '邻里互助：帮忙代收快递',
    likerNickname: '楼上张姐',
    likerAvatar: '/assets/default-avatar.png',
    likedAt: Date.now() - 2 * 24 * 60 * 60 * 1000
  },
  {
    id: 5,
    postTitle: '小区绿化建议征集',
    likerNickname: '爱花大姐',
    likerAvatar: '/assets/default-avatar.png',
    likedAt: Date.now() - 5 * 24 * 60 * 60 * 1000
  }
];

Page({
  data: {
    totalLikes: 0,    // 总获赞数
    records: [],      // 获赞记录列表
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

  // 加载获赞总数
  async loadStats() {
    try {
      const res = await userApi.getMyStats();
      const data = res.data || {};
      this.setData({ totalLikes: data.likeCount || data.totalLikes || 0 });
    } catch (err) {
      console.error('加载获赞统计失败', err);
    }
  },

  // 加载获赞记录（暂用模拟数据，后端接口就绪后替换）
  async loadRecords() {
    this.setData({ loading: true });
    try {
      // 模拟网络请求延迟
      await new Promise(resolve => setTimeout(resolve, 200));
      const records = MOCK_LIKE_RECORDS.map(item => ({
        ...item,
        timeText: formatRelativeTime(item.likedAt)
      }));
      this.setData({ records });
    } catch (err) {
      console.error('加载获赞记录失败', err);
    } finally {
      this.setData({ loading: false });
    }
  }
});
