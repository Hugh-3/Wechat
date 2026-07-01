// pages/notifications/index.js
// 消息通知页面

const notificationApi = require('../../api/notification');
const { formatRelativeTime } = require('../../utils/time');

// 通知类型映射
const TYPE_CONFIG = {
  1: { icon: '📢', text: '系统', color: '#07c160' },
  2: { icon: '💬', text: '评论', color: '#10aeff' },
  3: { icon: '🤝', text: '接单', color: '#ff976a' },
  4: { icon: '❤️', text: '点赞', color: '#fa5151' },
  5: { icon: '✅', text: '认证', color: '#07c160' }
};

Page({
  data: {
    notifications: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    unreadCount: 0,
    refreshing: false
  },

  onLoad() {
    this.loadNotifications(true);
    this.loadUnreadCount();
  },

  onShow() {
    // 页面再次显示时刷新未读数
    this.loadUnreadCount();
  },

  // 加载通知列表
  async loadNotifications(reset = false) {
    if (this.data.loading) return;
    if (reset) {
      this.setData({ notifications: [], page: 1, hasMore: true });
    }
    if (!this.data.hasMore && !reset) return;

    this.setData({ loading: true });

    try {
      const res = await notificationApi.getNotifications(this.data.page, this.data.pageSize);
      const result = res.data || { list: [], total: 0 };
      const list = (result.list || []).map(item => {
        const typeConfig = TYPE_CONFIG[item.type] || TYPE_CONFIG[1];
        return {
          ...item,
          typeIcon: typeConfig.icon,
          typeText: typeConfig.text,
          typeColor: typeConfig.color,
          timeText: formatRelativeTime(item.createdAt),
          isUnread: item.isRead === 0
        };
      });

      this.setData({
        notifications: [...this.data.notifications, ...list],
        page: this.data.page + 1,
        hasMore: (result.list || []).length >= this.data.pageSize
      });
    } catch (err) {
      console.error('加载通知失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false, refreshing: false });
    }
  },

  // 加载未读数量
  async loadUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount();
      const unreadCount = (res.data && res.data.unreadCount) || 0;
      this.setData({ unreadCount });
    } catch (err) {
      console.error('获取未读数失败', err);
    }
  },

  // 点击通知：标记已读并跳转到关联内容
  async onNotificationTap(e) {
    const { id, index, relatedtype, relatedid } = e.currentTarget.dataset;
    const notification = this.data.notifications[index];
    if (!notification) return;

    // 未读则标记为已读
    if (notification.isUnread) {
      try {
        await notificationApi.markAsRead(id);
        const notifications = [...this.data.notifications];
        notifications[index] = { ...notification, isUnread: false };
        this.setData({ notifications });
        // 未读数减1
        if (this.data.unreadCount > 0) {
          this.setData({ unreadCount: this.data.unreadCount - 1 });
        }
      } catch (err) {
        console.error('标记已读失败', err);
      }
    }

    // 根据关联类型跳转
    this.navigateByRelatedType(relatedtype, relatedid);
  },

  // 根据关联类型跳转
  navigateByRelatedType(relatedType, relatedId) {
    if (!relatedType || !relatedId) return;
    switch (relatedType) {
      case 'post':
        wx.navigateTo({ url: `/pages/post-detail/index?id=${relatedId}` });
        break;
      case 'order':
        wx.navigateTo({ url: `/pages/order-detail/index?id=${relatedId}` });
        break;
      case 'comment':
        // 评论无独立详情页，暂不跳转
        break;
      case 'user':
        // 用户无独立详情页，暂不跳转
        break;
      default:
        break;
    }
  },

  // 全部已读
  async onMarkAllRead() {
    if (this.data.unreadCount === 0) {
      wx.showToast({ title: '暂无未读通知', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '处理中...', mask: true });
    try {
      await notificationApi.markAllAsRead();
      const notifications = this.data.notifications.map(item => ({
        ...item,
        isUnread: false
      }));
      this.setData({ notifications, unreadCount: 0 });
      wx.showToast({ title: '已全部标记为已读', icon: 'success' });
    } catch (err) {
      console.error('全部已读失败', err);
      wx.showToast({ title: '操作失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ refreshing: true });
    Promise.all([
      this.loadNotifications(true),
      this.loadUnreadCount()
    ]).finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 上拉加载更多
  onReachBottom() {
    this.loadNotifications(false);
  }
});
