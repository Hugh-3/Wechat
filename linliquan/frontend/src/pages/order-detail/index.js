// pages/order-detail/index.js
// 订单详情页

const { getCurrentUserStatus, canWrite, formatDistance, getFuzzyDistance } = require('../../utils/auth');
const orderApi = require('../../api/order');
const { formatRelativeTime } = require('../../utils/time');

Page({
  data: {
    orderId: null,
    order: null,
    loading: true,
    userStatus: 0,
    canInteract: false,
    distanceText: '',
    action: ''
  },

  onLoad(options) {
    const orderId = options.id;
    const action = options.action || '';
    this.setData({ orderId, action });

    const status = getCurrentUserStatus();
    this.setData({
      userStatus: status,
      canInteract: canWrite()
    });

    this.loadOrderDetail();
  },

  onShow() {
    const status = getCurrentUserStatus();
    this.setData({
      userStatus: status,
      canInteract: canWrite()
    });
  },

  async loadOrderDetail() {
    this.setData({ loading: true });
    try {
      const res = await orderApi.getDetail(this.data.orderId);
      const order = res.data || {};
      order.timeText = formatRelativeTime(order.createdAt);

      let distanceText;
      if (this.data.canInteract) {
        distanceText = formatDistance(order.distance);
      } else {
        distanceText = getFuzzyDistance(order.distance);
      }

      this.setData({
        order,
        distanceText
      });

      if (this.data.action === 'contact') {
        setTimeout(() => {
          this.onContactTap();
        }, 500);
      }
    } catch (err) {
      console.error('加载订单详情失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onAcceptTap() {
    if (!this.data.canInteract) {
      this.showAuthModal();
      return;
    }

    wx.showModal({
      title: '确认接单',
      content: '确定要接下这个互助任务吗？',
      success: (res) => {
        if (res.confirm) {
          this.acceptOrder();
        }
      }
    });
  },

  async acceptOrder() {
    try {
      await orderApi.accept(this.data.orderId);
      wx.showToast({ title: '接单成功', icon: 'success' });
      this.setData({
        order: {
          ...this.data.order,
          status: 2,
          statusText: '进行中'
        }
      });
    } catch (err) {
      wx.showToast({ title: err.message || '接单失败', icon: 'none' });
    }
  },

  onCompleteTap() {
    wx.showModal({
      title: '确认完成',
      content: '确定任务已完成吗？',
      success: (res) => {
        if (res.confirm) {
          this.completeOrder();
        }
      }
    });
  },

  async completeOrder() {
    try {
      await orderApi.complete(this.data.orderId);
      wx.showToast({ title: '已完成', icon: 'success' });
      this.setData({
        order: {
          ...this.data.order,
          status: 3,
          statusText: '已完成'
        }
      });
    } catch (err) {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }
  },

  onContactTap() {
    if (!this.data.canInteract) {
      this.showAuthModal();
      return;
    }
    wx.showToast({ title: '联系功能开发中', icon: 'none' });
  },

  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.order.images || [];
    if (images.length === 0) return;
    wx.previewImage({
      current: images[index],
      urls: images
    });
  },

  showAuthModal() {
    wx.showModal({
      title: '需要业主认证',
      content: '认证业主后才能接单和联系\n\n🔒 安全认证，保护邻里',
      confirmText: '去认证',
      cancelText: '稍后再说',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/auth/index' });
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: this.data.order ? this.data.order.title : '邻里互助',
      path: `/pages/order-detail/index?id=${this.data.orderId}`
    };
  }
});
