/**
 * 邻里圈小程序 - HTTP请求封装
 * 支持：多环境切换、Token自动注入、响应拦截、错误处理、权限引导、Mock模式
 */

const envConfig = require('./env.js');
const mockData = require('../mock/data.js');

function getApiBaseUrl() {
  return envConfig.getApiBaseUrl();
}

const DEFAULT_CONFIG = {
  timeout: 30000,
  showLoading: true,
  loadingText: '加载中...',
  retryCount: 1
};

// 请求计数器（用于日志追踪）
let requestIndex = 0;

/**
 * 核心请求方法
 * @param {string} url - 请求路径
 * @param {object} options - 请求配置
 */
function request(url, options = {}) {
  const config = { ...DEFAULT_CONFIG, ...options };
  const token = wx.getStorageSync('accessToken');
  const currentRequestId = ++requestIndex;

  if (envConfig.isMockMode()) {
    return mockRequest(url, config);
  }

  const baseUrl = getApiBaseUrl();

  return new Promise((resolve, reject) => {
    if (config.showLoading !== false) {
      wx.showLoading({
        title: config.loadingText || '加载中...',
        mask: true
      });
    }

    if (envConfig.isDebug()) {
      console.log(`[API] ${config.method || 'GET'} ${baseUrl}${url}`, config.data || '');
    }

    const requestPayload = {
      url: baseUrl + url,
      method: config.method || 'GET',
      data: config.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'X-Request-Id': `req_${currentRequestId}_${Date.now()}`
      },
      timeout: config.timeout,
      success: (res) => {
        if (envConfig.isDebug()) {
          console.log(`[API] Response ${baseUrl}${url}`, res.statusCode, res.data);
        }
        handleSuccess(res, config, resolve, reject);
      },
      fail: (err) => {
        if (envConfig.isDebug()) {
          console.error(`[API] Failed ${baseUrl}${url}`, err);
        }
        handleFail(err, config, url, reject);
      }
    };

    wx.request(requestPayload);
  });
}

function mockRequest(url, config) {
  return new Promise((resolve, reject) => {
    if (config.showLoading !== false) {
      wx.showLoading({
        title: config.loadingText || '加载中...',
        mask: true
      });
    }

    setTimeout(() => {
      wx.hideLoading();
      const method = (config.method || 'GET').toUpperCase();
      const mockResult = mockData.handleMock(url, method, config.data);

      if (envConfig.isDebug()) {
        console.log(`[Mock] ${method} ${url}`, mockResult);
      }

      if (mockResult && mockResult.success === true) {
        resolve(mockResult);
      } else {
        if (mockResult && mockResult.code) {
          handleBusinessError(mockResult, reject);
        } else {
          reject(mockResult || { success: false, message: 'Mock数据未找到' });
        }
      }
    }, 300 + Math.random() * 500);
  });
}

/**
 * 处理成功响应
 */
function handleSuccess(res, config, resolve, reject) {
  wx.hideLoading();

  const { statusCode, data } = res;

  // HTTP 200
  if (statusCode === 200) {
    // 业务成功
    if (data && data.success === true) {
      resolve(data);
    }
    // 业务失败
    else if (data) {
      handleBusinessError(data, reject);
    }
    else {
      reject({ success: false, message: '响应数据格式错误' });
    }
  }
  // HTTP 401 - 未登录
  else if (statusCode === 401) {
    handleUnauthorized(data, reject);
  }
  // HTTP 403 - 无权限
  else if (statusCode === 403) {
    handleForbidden(data, reject);
  }
  // HTTP 404 - 资源不存在
  else if (statusCode === 404) {
    reject({ success: false, message: '请求的资源不存在', code: 404 });
  }
  // HTTP 500 - 服务器错误
  else if (statusCode >= 500) {
    handleServerError(statusCode, reject);
  }
  // 其他错误
  else {
    reject({ success: false, message: `请求失败(${statusCode})`, code: statusCode });
  }
}

/**
 * 处理业务错误码
 */
function handleBusinessError(data, reject) {
  const { code, message } = data;

  // 根据错误码处理
  switch (code) {
    case 40301: // 未认证
    case 40302: // 认证中
    case 40303: // 未登录
      handleForbidden(data, reject);
      break;
    case 404:
      wx.showToast({ title: '资源不存在', icon: 'none' });
      reject(data);
      break;
    case 409:
      wx.showToast({ title: message || '资源冲突', icon: 'none' });
      reject(data);
      break;
    default:
      wx.showToast({ title: message || '操作失败', icon: 'none' });
      reject(data);
  }
}

/**
 * 处理403权限错误
 */
function handleForbidden(data, reject) {
  const { code } = data;

  switch (code) {
    case 40301:
      // 仅认证业主可操作
      wx.showModal({
        title: '无法操作',
        content: '仅认证业主可进行此操作',
        confirmText: '去认证',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/auth/index' });
          }
        }
      });
      break;
    case 40302:
      // 认证审核中
      wx.showModal({
        title: '认证审核中',
        content: '您的业主认证正在审核中，审核通过后即可使用全部功能',
        showCancel: false
      });
      break;
    case 40303:
      // 未登录
      wx.showModal({
        title: '请先登录',
        content: '请先完成业主认证',
        confirmText: '去认证',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/auth/index' });
          }
        }
      });
      break;
    default:
      wx.showToast({ title: '无权限访问', icon: 'none' });
  }

  reject(data);
}

/**
 * 处理401未登录
 */
function handleUnauthorized(data, reject) {
  // 清除Token
  wx.removeStorageSync('accessToken');
  wx.removeStorageSync('userInfo');

  wx.showModal({
    title: '登录已过期',
    content: '请重新登录',
    confirmText: '重新登录',
    success: (res) => {
      if (res.confirm) {
        wx.navigateTo({ url: '/pages/auth/index' });
      }
    }
  });

  reject({ success: false, message: '登录已过期', code: 401 });
}

/**
 * 处理服务器错误
 */
function handleServerError(statusCode, reject) {
  console.error(`Server Error: ${statusCode}`);
  wx.showToast({
    title: '服务器繁忙，请稍后再试',
    icon: 'none',
    duration: 3000
  });
  reject({ success: false, message: '服务器错误', code: statusCode });
}

/**
 * 处理请求失败
 */
function handleFail(err, config, url, reject) {
  wx.hideLoading();
  console.error(`Request Failed: ${url}`, err);

  if (err.errMsg && err.errMsg.includes('abort')) {
    reject({ success: false, message: '请求被取消' });
    return;
  }

  wx.showToast({
    title: '网络错误，请检查网络连接',
    icon: 'none',
    duration: 2000
  });

  reject({ success: false, message: '网络错误', error: err });
}

/**
 * GET请求
 */
function get(url, data, config = {}) {
  return request(url, { ...config, method: 'GET', data });
}

/**
 * POST请求
 */
function post(url, data, config = {}) {
  return request(url, { ...config, method: 'POST', data });
}

/**
 * PUT请求
 */
function put(url, data, config = {}) {
  return request(url, { ...config, method: 'PUT', data });
}

/**
 * DELETE请求
 */
function del(url, data, config = {}) {
  return request(url, { ...config, method: 'DELETE', data });
}

/**
 * 上传文件
 */
function uploadFile(filePath, name = 'file', formData = {}) {
  const token = wx.getStorageSync('accessToken');
  const baseUrl = getApiBaseUrl();

  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '上传中...', mask: true });

    wx.uploadFile({
      url: baseUrl + '/upload',
      filePath,
      name,
      formData,
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          const data = JSON.parse(res.data);
          if (data.success) {
            resolve(data);
          } else {
            reject(data);
          }
        } else {
          wx.showToast({ title: '上传失败', icon: 'none' });
          reject({ success: false, message: '上传失败' });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({ title: '上传失败', icon: 'none' });
        reject(err);
      }
    });
  });
}

/**
 * 获取当前位置
 */
function getLocation() {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude
        });
      },
      fail: (err) => {
        // 用户拒绝授权
        if (err.errMsg && err.errMsg.includes('auth deny')) {
          wx.showModal({
            title: '需要位置权限',
            content: '您的位置信息将用于附近互助任务的定位展示',
            confirmText: '去授权',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting();
              }
            }
          });
        }
        reject(err);
      }
    });
  });
}

module.exports = {
  request,
  get,
  post,
  put,
  delete: del,
  uploadFile,
  getLocation,
  API_BASE_URL: getApiBaseUrl(),
  getApiBaseUrl,
  envConfig
};
