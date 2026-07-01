/**
 * 邻里圈小程序 - 环境配置
 * 支持: 开发环境 / 测试环境 / 生产环境 / Mock环境
 */

const ENV_MOCK = 'mock';
const ENV_DEV = 'dev';
const ENV_TEST = 'test';
const ENV_PROD = 'prod';

const ENV_CONFIG = {
  [ENV_MOCK]: {
    name: 'Mock环境',
    apiBaseUrl: '',
    useMock: true,
    debug: true
  },
  [ENV_DEV]: {
    name: '开发环境',
    apiBaseUrl: 'http://localhost:8080/api',
    useMock: false,
    debug: true
  },
  [ENV_TEST]: {
    name: '测试环境',
    apiBaseUrl: 'https://test-api.linliquan.com/api',
    useMock: false,
    debug: true
  },
  [ENV_PROD]: {
    name: '生产环境',
    apiBaseUrl: 'https://api.linliquan.com/api',
    useMock: false,
    debug: false
  }
};

const DEFAULT_ENV = ENV_DEV;
const ENV_STORAGE_KEY = 'app_env';

function getCurrentEnv() {
  try {
    const savedEnv = wx.getStorageSync(ENV_STORAGE_KEY);
    if (savedEnv && ENV_CONFIG[savedEnv]) {
      return savedEnv;
    }
  } catch (e) {
  }
  return DEFAULT_ENV;
}

function setCurrentEnv(env) {
  if (ENV_CONFIG[env]) {
    try {
      wx.setStorageSync(ENV_STORAGE_KEY, env);
    } catch (e) {
    }
    return true;
  }
  return false;
}

function getConfig() {
  const env = getCurrentEnv();
  return ENV_CONFIG[env];
}

function getApiBaseUrl() {
  return getConfig().apiBaseUrl;
}

function isMockMode() {
  return getConfig().useMock;
}

function isDebug() {
  return getConfig().debug;
}

function listEnvs() {
  return Object.keys(ENV_CONFIG).map(key => ({
    key,
    name: ENV_CONFIG[key].name,
    isMock: ENV_CONFIG[key].useMock
  }));
}

module.exports = {
  ENV_MOCK,
  ENV_DEV,
  ENV_TEST,
  ENV_PROD,
  getCurrentEnv,
  setCurrentEnv,
  getConfig,
  getApiBaseUrl,
  isMockMode,
  isDebug,
  listEnvs
};
