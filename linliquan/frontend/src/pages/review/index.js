// pages/review/index.js
// 评价页面

const reviewApi = require('../../api/review');
const { formatRelativeTime } = require('../../utils/time');

Page({
  data: {
    orderId: null,
    // 评价表单
    rating: 0,
    content: '',
    images: [],
    submitting: false,
    // 评价列表
    reviews: [],
    canReview: false,
    canReviewReason: '',
    loading: true,
    stars: [1, 2, 3, 4, 5]
  },

  onLoad(options) {
    const orderId = options.id ? Number(options.id) : null;
    this.setData({ orderId });
    this.loadPageData();
  },

  async loadPageData() {
    this.setData({ loading: true });
    try {
      await Promise.all([
        this.loadReviews(),
        this.loadCanReview()
      ]);
    } catch (err) {
      console.error('加载评价页数据失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadReviews() {
    try {
      const res = await reviewApi.getOrderReviews(this.data.orderId);
      const reviews = (res.data || []).map((item) => {
        return {
          ...item,
          timeText: formatRelativeTime(item.createdAt),
          stars: this.buildStars(item.rating)
        };
      });
      this.setData({ reviews });
    } catch (err) {
      console.error('加载评价列表失败', err);
    }
  },

  async loadCanReview() {
    try {
      const res = await reviewApi.canReview(this.data.orderId);
      const data = res.data || {};
      this.setData({
        canReview: !!data.canReview,
        canReviewReason: data.reason || ''
      });
    } catch (err) {
      // 未登录等情况
      this.setData({
        canReview: false,
        canReviewReason: '请先登录'
      });
    }
  },

  buildStars(rating) {
    const arr = [];
    for (let i = 1; i <= 5; i++) {
      arr.push(i <= rating);
    }
    return arr;
  },

  onStarTap(e) {
    const star = Number(e.currentTarget.dataset.star);
    this.setData({ rating: star });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  chooseImage() {
    const remain = 3 - this.data.images.length;
    if (remain <= 0) {
      wx.showToast({ title: '最多上传3张图片', icon: 'none' });
      return;
    }
    wx.chooseImage({
      count: remain,
      success: (res) => {
        const newImages = [...this.data.images, ...res.tempFilePaths].slice(0, 3);
        this.setData({ images: newImages });
      }
    });
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = [...this.data.images];
    images.splice(index, 1);
    this.setData({ images });
  },

  previewFormImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images;
    if (!images.length) return;
    wx.previewImage({
      current: images[index],
      urls: images
    });
  },

  previewReviewImage(e) {
    const { index, imgIndex } = e.currentTarget.dataset;
    const review = this.data.reviews[index];
    if (!review || !review.images || !review.images.length) return;
    wx.previewImage({
      current: review.images[imgIndex],
      urls: review.images
    });
  },

  async submitReview() {
    const { orderId, rating, content, images } = this.data;

    if (!orderId) {
      wx.showToast({ title: '订单不存在', icon: 'none' });
      return;
    }
    if (rating < 1 || rating > 5) {
      wx.showToast({ title: '请选择评分', icon: 'none' });
      return;
    }
    if (!content || content.trim().length === 0) {
      wx.showToast({ title: '请填写评价内容', icon: 'none' });
      return;
    }
    if (content.length > 500) {
      wx.showToast({ title: '评价内容不能超过500字', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      await reviewApi.createReview(orderId, rating, content.trim(), images);
      wx.showToast({ title: '评价成功', icon: 'success' });
      this.setData({
        rating: 0,
        content: '',
        images: []
      });
      // 刷新评价列表与可评价状态
      await this.loadPageData();
    } catch (err) {
      wx.showToast({ title: err.message || '评价失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  goBack() {
    wx.navigateBack();
  }
});
