// pages/my-orders/index.js
// 我的互助页面 - 展示当前用户参与的互助任务

const orderApi = require('../../api/order');
const { formatRelativeTime } = require('../../utils/time');
const { getCurrentUserStatus } = require('../../utils/auth');

// 互助状态映射：1-待接单, 2-进行中, 3-已完成, 4-已取消
// color 使用 6 位 hex，便于在 wxml 中拼接 alpha 通道实现浅色背景
const STATUS_MAP = {
  1: { text: '待接单', color: '#ff976a' },
  2: { text: '进行中', color: '#10aeff' },
  3: { text: '已完成', color: '#07c160' },
  4: { text: '已取消', color: '#999999' }
};

// 互助类型映射：1-拼单团购, 2-代取代买, 3-生活求助, 4-技能交换
const HELP_TYPE_MAP = {
  1: '拼单团购',
  2: '代取代买',
  3: '生活求助',
  4: '技能交换'
};

// Tab 配置：role 对应后端 role 参数
const TABS = [
  { key: 'all', name: '全部' },
  { key: 'published', name: '我发布的' },
  { key: 'helped', name: '我帮助的' }
];

Page({
  data: {
    orders: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    activeTab: 'all',  // all, published, helped
    userStatus: 0,
    tabs: TABS
  },

  onLoad() {
    // 读取当前用户认证状态
    const userStatus = getCurrentUserStatus();
    this.setData({ userStatus });
    this.loadOrders(true);
  },

  onShow() {
    // 页面再次显示时刷新认证状态
    const userStatus = getCurrentUserStatus();
    this.setData({ userStatus });
  },

  // 加载互助任务列表
  async loadOrders(reset = false) {
    if (this.data.loading) return;
    if (reset) {
      this.setData({ orders: [], page: 1, hasMore: true });
    }
    if (!this.data.hasMore && !reset) return;

    this.setData({ loading: true });

    try {
      const res = await orderApi.getMyOrders(
        this.data.activeTab,
        this.data.page,
        this.data.pageSize
      );
      const result = res.data || { list: [] };
      const list = (result.list || []).map(order => {
        const statusConfig = STATUS_MAP[order.status] || STATUS_MAP[1];
        return {
          ...order,
          statusText: statusConfig.text,
          statusColor: statusConfig.color,
          helpTypeName: HELP_TYPE_MAP[order.helpType] || '其他',
          timeText: formatRelativeTime(order.createdAt)
        };
      });

      this.setData({
        orders: [...this.data.orders, ...list],
        page: this.data.page + 1,
        hasMore: (result.list || []).length >= this.data.pageSize
      });
    } catch (err) {
      console.error('加载互助列表失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 切换 Tab
  onTabChange(e) {
    const key = e.currentTarget.dataset.key;
    if (key === this.data.activeTab) return;
    this.setData({ activeTab: key });
    this.loadOrders(true);
  },

  // 点击任务跳转详情
  onOrderTap(e) {
    const orderId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/order-detail/index?id=${orderId}` });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadOrders(true).finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 上拉加载更多
  onReachBottom() {
    this.loadOrders(false);
  }
});
