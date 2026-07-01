// pages/publish/index.js
// 发布页面

const { getCurrentUserStatus, canWrite } = require('../../utils/auth');
const postApi = require('../../api/post');

Page({
  data: {
    postType: 1,
    title: '',
    content: '',
    images: [],
    latitude: null,
    longitude: null,
    helpType: 1,
    rewardAmount: '',
    helpTypes: [
      { id: 1, name: '拼单团购' },
      { id: 2, name: '代取代买' },
      { id: 3, name: '生活求助' },
      { id: 4, name: '技能交换' }
    ],
    submitting: false
  },

  onLoad(options) {
    const type = options && options.type;
    if (type === 'help') {
      this.setData({ postType: 2 });
      this.getLocation();
    }
  },

  onPostTypeChange(e) {
    const type = parseInt(e.currentTarget.dataset.type);
    this.setData({ postType: type });
    if (type === 2) {
      this.getLocation();
    }
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  onHelpTypeChange(e) {
    this.setData({ helpType: parseInt(e.currentTarget.dataset.id) });
  },

  onRewardInput(e) {
    this.setData({ rewardAmount: e.detail.value });
  },

  getLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          latitude: res.latitude,
          longitude: res.longitude
        });
      },
      fail: () => {
        wx.showToast({ title: '获取位置失败', icon: 'none' });
      }
    });
  },

  chooseImage() {
    wx.chooseImage({
      count: 9,
      success: (res) => {
        const newImages = [...this.data.images, ...res.tempFilePaths];
        this.setData({ images: newImages.slice(0, 9) });
      }
    });
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = [...this.data.images];
    images.splice(index, 1);
    this.setData({ images });
  },

  submitPost() {
    const { title, content, postType, latitude, longitude, helpType, rewardAmount } = this.data;

    if (!title || title.trim().length < 5) {
      wx.showToast({ title: '标题至少5个字符', icon: 'none' });
      return;
    }

    if (!content || content.trim().length < 10) {
      wx.showToast({ title: '内容至少10个字符', icon: 'none' });
      return;
    }

    if (postType === 2 && (!latitude || !longitude)) {
      wx.showToast({ title: '互助任务需要位置信息', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    const params = {
      title: title.trim(),
      content: content.trim(),
      type: postType,
      latitude,
      longitude,
      images: this.data.images
    };

    if (postType === 2) {
      params.helpType = helpType;
      params.rewardAmount = rewardAmount ? parseFloat(rewardAmount) : 0;
    }

    postApi.create(params).then(() => {
      wx.showToast({ title: '发布成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }).catch((err) => {
      wx.showToast({ title: err.message || '发布失败', icon: 'none' });
    }).finally(() => {
      this.setData({ submitting: false });
    });
  },

  goBack() {
    wx.navigateBack();
  }
});
