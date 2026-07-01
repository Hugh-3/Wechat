// pages/helper/index.js
// 邻里互助页面

const { getCurrentUserStatus, getStatusConfig, getFuzzyDistance, formatDistance } = require('../../utils/auth');
const orderApi = require('../../api/order');

Page({
  data: {
    orders: [],
    page: 1,
    hasMore: true,
    loading: false,
    userStatus: 0,
    userStatusDesc: '',
    publishEnabled: false,
    helpTypes: [
      { id: 0, name: '全部' },
      { id: 1, name: '拼单团购' },
      { id: 2, name: '代取代买' },
      { id: 3, name: '生活求助' },
      { id: 4, name: '技能交换' }
    ],
    activeType: 0,
    location: null,
    radius: 5,
  },

  onLoad() {
    const status = getCurrentUserStatus();
    const config = getStatusConfig(status);
    this.setData({
      userStatus: status,
      userStatusDesc: config.label,
      publishEnabled: config.canWrite
    });

    this.getLocation();
  },

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
        wx.showModal({
          title: '提示',
          content: '无法获取位置信息，将按默认位置展示',
          showCancel: false
        });
        this.setData({
          location: { lat: 23.1291, lng: 113.2644 }
        });
        this.loadNearbyOrders();
      }
    });
  },

  async loadNearbyOrders() {
    if (this.data.loading) return;

    const { lat, lng } = this.data.location;
    if (!lat || !lng) return;

    this.setData({ loading: true });

    try {
      const res = await orderApi.getNearby(lat, lng, this.data.radius);
      const result = res.data || { list: [], total: 0 };
      let orders = result.list || [];

      orders = orders.map(order => {
        let distanceText;
        if (!this.data.publishEnabled) {
          distanceText = getFuzzyDistance(order.distance);
        } else {
          distanceText = formatDistance(order.distance);
        }
        return {
          ...order,
          distanceText
        };
      });

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

  onTypeChange(e) {
    const type = e.currentTarget.dataset.id;
    this.setData({ activeType: type });
    this.loadNearbyOrders();
  },

  onOrderTap(e) {
    const orderId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/order-detail/index?id=${orderId}` });
  },

  onContactTap(e) {
    const orderId = e.currentTarget.dataset.id;

    if (!this.data.publishEnabled) {
      this.showAuthGuideModal();
      return;
    }

    wx.navigateTo({ url: `/pages/order-detail/index?id=${orderId}&action=contact` });
  },

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
