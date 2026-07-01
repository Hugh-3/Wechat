// pages/points/index.js
// 积分中心页面 - 积分明细 / 积分商城 / 兑换记录

const pointsApi = require('../../api/points');
const { formatRelativeTime } = require('../../utils/time');

// 积分动作描述映射
const ACTION_DESC_MAP = {
  post_create: '发布动态',
  order_publish: '发布互助任务',
  order_accept: '接单奖励',
  order_complete: '完成互助任务',
  like_received: '获得点赞',
  comment_received: '收到评论',
  daily_login: '每日登录',
  exchange: '积分兑换'
};

Page({
  data: {
    totalPoints: 0,
    activeTab: 'transactions',
    tabs: [
      { key: 'transactions', name: '积分明细' },
      { key: 'shop', name: '积分商城' },
      { key: 'records', name: '兑换记录' }
    ],
    // 积分明细
    transactions: [],
    txnPage: 1,
    txnPageSize: 10,
    txnHasMore: true,
    txnLoading: false,
    // 积分商城
    items: [],
    itemPage: 1,
    itemPageSize: 10,
    itemHasMore: true,
    itemLoading: false,
    // 兑换记录
    records: [],
    recordPage: 1,
    recordPageSize: 10,
    recordHasMore: true,
    recordLoading: false
  },

  onLoad() {
    this.loadBalance();
    this.loadTransactions();
  },

  onShow() {
    this.loadBalance();
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.refreshCurrentTab().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 切换Tab
  onTabChange(e) {
    const key = e.currentTarget.dataset.key;
    if (key === this.data.activeTab) return;
    this.setData({ activeTab: key });

    // 懒加载：首次切换到该Tab时加载数据
    if (key === 'shop' && this.data.items.length === 0) {
      this.loadExchangeItems();
    } else if (key === 'records' && this.data.records.length === 0) {
      this.loadExchangeRecords();
    }
  },

  // 刷新当前Tab
  refreshCurrentTab() {
    this.loadBalance();
    const tab = this.data.activeTab;
    if (tab === 'transactions') {
      this.setData({ transactions: [], txnPage: 1, txnHasMore: true });
      return this.loadTransactions();
    } else if (tab === 'shop') {
      this.setData({ items: [], itemPage: 1, itemHasMore: true });
      return this.loadExchangeItems();
    } else if (tab === 'records') {
      this.setData({ records: [], recordPage: 1, recordHasMore: true });
      return this.loadExchangeRecords();
    }
    return Promise.resolve();
  },

  // 加载积分余额
  async loadBalance() {
    try {
      const res = await pointsApi.getBalance();
      const data = res.data || {};
      this.setData({ totalPoints: data.totalPoints || 0 });
    } catch (err) {
      console.error('加载余额失败', err);
    }
  },

  // ==================== 积分明细 ====================

  async loadTransactions() {
    if (this.data.txnLoading || !this.data.txnHasMore) return;
    this.setData({ txnLoading: true });
    try {
      const res = await pointsApi.getTransactions(this.data.txnPage, this.data.txnPageSize);
      const result = res.data || { list: [] };
      const list = (result.list || []).map(item => ({
        ...item,
        actionDesc: ACTION_DESC_MAP[item.action] || item.action,
        timeText: formatRelativeTime(item.createdAt),
        pointsText: (item.type === 1 ? '+' : '-') + item.points
      }));
      this.setData({
        transactions: [...this.data.transactions, ...list],
        txnPage: this.data.txnPage + 1,
        txnHasMore: (result.list || []).length >= this.data.txnPageSize
      });
    } catch (err) {
      console.error('加载明细失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ txnLoading: false });
    }
  },

  onTxnLoadMore() {
    this.loadTransactions();
  },

  // ==================== 积分商城 ====================

  async loadExchangeItems() {
    if (this.data.itemLoading || !this.data.itemHasMore) return;
    this.setData({ itemLoading: true });
    try {
      const res = await pointsApi.getExchangeItems(this.data.itemPage, this.data.itemPageSize);
      const result = res.data || { list: [] };
      const list = (result.list || []).map(item => ({
        ...item,
        canExchange: item.stock > 0 && this.data.totalPoints >= item.pointsRequired
      }));
      this.setData({
        items: [...this.data.items, ...list],
        itemPage: this.data.itemPage + 1,
        itemHasMore: (result.list || []).length >= this.data.itemPageSize
      });
    } catch (err) {
      console.error('加载商品失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ itemLoading: false });
    }
  },

  onItemLoadMore() {
    this.loadExchangeItems();
  },

  // 兑换商品
  onExchange(e) {
    const item = e.currentTarget.dataset.item;
    if (item.stock <= 0) {
      wx.showToast({ title: '库存不足', icon: 'none' });
      return;
    }
    if (this.data.totalPoints < item.pointsRequired) {
      wx.showToast({ title: '积分不足', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '确认兑换',
      content: `是否使用 ${item.pointsRequired} 积分兑换「${item.name}」？`,
      confirmText: '兑换',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await pointsApi.exchangeItem(item.id);
          wx.showToast({ title: '兑换成功', icon: 'success' });
          // 刷新余额和商品列表
          await this.loadBalance();
          this.setData({ items: [], itemPage: 1, itemHasMore: true });
          this.loadExchangeItems();
        } catch (err) {
          console.error('兑换失败', err);
        }
      }
    });
  },

  // ==================== 兑换记录 ====================

  async loadExchangeRecords() {
    if (this.data.recordLoading || !this.data.recordHasMore) return;
    this.setData({ recordLoading: true });
    try {
      const res = await pointsApi.getExchangeRecords(this.data.recordPage, this.data.recordPageSize);
      const result = res.data || { list: [] };
      const statusMap = { 1: '待发放', 2: '已发放', 3: '已取消' };
      const statusClassMap = { 1: 'pending', 2: 'done', 3: 'cancelled' };
      const list = (result.list || []).map(item => ({
        ...item,
        statusText: statusMap[item.status] || '未知',
        statusClass: statusClassMap[item.status] || '',
        timeText: formatRelativeTime(item.createdAt)
      }));
      this.setData({
        records: [...this.data.records, ...list],
        recordPage: this.data.recordPage + 1,
        recordHasMore: (result.list || []).length >= this.data.recordPageSize
      });
    } catch (err) {
      console.error('加载兑换记录失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ recordLoading: false });
    }
  },

  onRecordLoadMore() {
    this.loadExchangeRecords();
  }
});
