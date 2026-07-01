// pages/helper/index.js
// 邻里互助页面

const { getCurrentUserStatus, getStatusConfig, getFuzzyDistance } = require('../../utils/auth');
const { getNearbyOrders } = require('../../mock/data');

Page({
  data: {
    orders: [],
    page: 1,
    hasMore: true,
    loading: false,
    // 用户状态
    userStatus: 0,
    userStatusDesc: '',
    publishEnabled: false,
    // 筛选
    helpTypes: [
      { id: 0, name: '全部' },
      { id: 1, name: '拼单团购' },
      { id: 2, name: '代取代买' },
      { id: 3, name: '生活求助' },
      { id: 4, name: '技能交换' }
    ],
    activeType: 0,
    // 位置
    location: null,
    radius: 5, // 公里
  },

  onLoad() {
    const status = getCurrentUserStatus();
    const config = getStatusConfig(status);
    this.setData({
      userStatus: status,
      userStatusDesc: config.label,
      publishEnabled: config.canWrite
    });

    // 获取当前位置
    this.getLocation();
  },

  // 获取当前位置
  getLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          location: {
            lat: res.latitude,
            lng: res.longitude
          }
        });
        this.loadNearbyOrders();
      },
      fail: () => {
        // 定位失败，提示用户
        wx.showModal({
          title: '提示',
          content: '无法获取位置信息，将按默认位置展示',
          showCancel: false
        });
        // 使用默认位置（城市中心）
        this.setData({
          location: { lat: 23.1291, lng: 113.2644 } // 默认广州
        });
        this.loadNearbyOrders();
      }
    });
  },

  // 加载附近互助任务
  async loadNearbyOrders() {
    if (this.data.loading) return;

    const { lat, lng } = this.data.location;
    if (!lat || !lng) return;

    this.setData({ loading: true });

    try {
      const result = await getNearbyOrders(lat, lng, this.data.radius);
      let orders = result.list || [];

      // 非认证用户：模糊距离
      if (!this.data.publishEnabled) {
        orders = orders.map(order => ({
          ...order,
          distanceText: getFuzzyDistance(order.distanceText)
        }));
      }

      this.setData({
        orders: orders,
        hasMore: false
      });
    } catch (err) {
      console.error('加载失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  // 切换互助类型
  onTypeChange(e) {
    const type = e.currentTarget.dataset.id;
    this.setData({ activeType: type });
    this.loadNearbyOrders();
  },

  // 点击任务卡片
  onOrderTap(e) {
    const orderId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/order-detail/index?id=${orderId}` });
  },

  // 我要联系
  onContactTap(e) {
    const orderId = e.currentTarget.dataset.id;

    if (!this.data.publishEnabled) {
      // 【红线强制】UNAUTH状态：弹出全屏认证引导
      this.showAuthGuideModal();
      return;
    }

    // VERIFIED状态：跳转详情页联系
    wx.navigateTo({ url: `/pages/order-detail/index?id=${orderId}&action=contact` });
  },

  // 【红线强制】全屏认证引导弹窗
  showAuthGuideModal() {
    wx.showModal({
      title: '为了保护您和邻居的安全',
      content: '只有认证业主才能查看联系方式\n\n🔒 您的证件信息将加密存储\n📋 审核通过后7天自动删除原图\n✅ 认证通过即可联系邻居',
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

  // 切换用户状态（开发调试）
  switchStatus() {
    const statuses = [0, 1, 2];
    const current = this.data.userStatus;
    const nextIndex = (statuses.indexOf(current) + 1) % statuses.length;
    wx.setStorageSync('mockUserStatus', statuses[nextIndex]);
    this.onLoad();
  },

  onPullDownRefresh() {
    this.loadNearbyOrders();
    wx.stopPullDownRefresh();
  }
});
