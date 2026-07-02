// pages/edit-profile/index.js
// 资料编辑页面，用于修改用户头像和昵称

const userApi = require('../../api/user');
const { uploadFile } = require('../../utils/request');

Page({
  data: {
    nickname: '',        // 昵称
    avatarUrl: '',      // 头像URL
    submitting: false   // 是否正在提交
  },

  onLoad() {
    // 从本地存储读取当前用户信息
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({
      nickname: userInfo.nickname || '',
      avatarUrl: userInfo.avatar || userInfo.avatarUrl || ''
    });
  },

  // 昵称输入
  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  // 选择头像图片并上传
  async onChooseAvatar() {
    try {
      // 调用微信API选择图片
      const chooseRes = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: reject
        });
      });

      const tempFilePath = chooseRes.tempFiles[0].tempFilePath;
      // 先展示本地预览，提升体验
      this.setData({ avatarUrl: tempFilePath });

      // 上传到服务器获取URL
      const uploadRes = await uploadFile(tempFilePath, 'file');
      const url = (uploadRes.data && (uploadRes.data.url || uploadRes.data.avatarUrl)) || '';
      if (url) {
        this.setData({ avatarUrl: url });
      }
    } catch (err) {
      // 用户取消选择时不需要提示
      if (err && err.errMsg && err.errMsg.indexOf('cancel') >= 0) return;
      console.error('头像上传失败', err);
      wx.showToast({ title: '头像上传失败', icon: 'none' });
    }
  },

  // 保存资料
  async onSave() {
    const { nickname, avatarUrl, submitting } = this.data;

    if (submitting) return;

    const trimmed = (nickname || '').trim();
    if (!trimmed) {
      wx.showToast({ title: '请输入昵称', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    try {
      await userApi.updateProfile({ nickname: trimmed, avatarUrl });
      // 同步更新本地存储的用户信息
      const userInfo = wx.getStorageSync('userInfo') || {};
      userInfo.nickname = trimmed;
      userInfo.avatar = avatarUrl;
      userInfo.avatarUrl = avatarUrl;
      wx.setStorageSync('userInfo', userInfo);

      wx.showToast({ title: '保存成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } catch (err) {
      console.error('保存资料失败', err);
      wx.showToast({ title: (err && err.message) || '保存失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  }
});
