const { setCurrentUserStatus, UserStatus } = require('../../utils/auth');

Page({
  data: {
    phone: '',
    idCard: '',
    houseNumber: '',
    step: 1,
    submitting: false
  },

  onLoad() {
    const user = wx.getStorageSync('mockUser');
    if (user) {
      this.setData({ phone: user.phone || '' });
    }
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  onIdCardInput(e) {
    this.setData({ idCard: e.detail.value });
  },

  onHouseNumberInput(e) {
    this.setData({ houseNumber: e.detail.value });
  },

  goToStep2() {
    const { phone } = this.data;
    if (!phone || phone.length !== 11) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    this.setData({ step: 2 });
  },

  submitVerification() {
    const { idCard, houseNumber, phone } = this.data;

    if (!idCard || idCard.length !== 18) {
      wx.showToast({ title: '请输入正确的身份证号', icon: 'none' });
      return;
    }

    if (!houseNumber) {
      wx.showToast({ title: '请输入房号', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    setTimeout(() => {
      wx.showModal({
        title: '提交成功',
        content: '您的业主认证申请已提交，审核员将在1-2个工作日内完成审核',
        showCancel: false,
        success: () => {
          setCurrentUserStatus(UserStatus.PENDING);
          wx.switchTab({ url: '/pages/index/index' });
        }
      });
      this.setData({ submitting: false });
    }, 1500);
  },

  goBack() {
    if (this.data.step === 2) {
      this.setData({ step: 1 });
    } else {
      wx.navigateBack();
    }
  }
});
