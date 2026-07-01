const API_BASE_URL = 'http://localhost:8080/api';

function request(url, options = {}) {
  const token = wx.getStorageSync('accessToken');

  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '加载中...' });

    wx.request({
      url: API_BASE_URL + url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data);
          } else {
            if (res.statusCode === 403) {
              handleForbidden(res.data.code);
            }
            reject(res.data);
          }
        } else if (res.statusCode === 403) {
          wx.hideLoading();
          handleForbidden(res.data.code);
          reject(res.data);
        } else {
          wx.hideLoading();
          wx.showToast({ title: '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
}

function handleForbidden(code) {
  if (code === 40301) {
    wx.showModal({
      title: '提示',
      content: '您的业主认证正在审核中',
      showCancel: false
    });
  } else if (code === 40303) {
    wx.showModal({
      title: '提示',
      content: '请先完成业主认证',
      confirmText: '去认证',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/auth/index' });
        }
      }
    });
  }
}

module.exports = {
  request,
  get: (url, data) => request(url, { method: 'GET', data }),
  post: (url, data) => request(url, { method: 'POST', data }),
  put: (url, data) => request(url, { method: 'PUT', data }),
  delete: (url, data) => request(url, { method: 'DELETE', data })
};
