const { get, post } = require('../utils/request');

/**
 * 获取当前用户积分余额
 */
function getBalance() {
  return get('/v1/points/balance');
}

/**
 * 获取积分流水记录（分页）
 */
function getTransactions(page, pageSize) {
  return get('/v1/points/transactions', { page, pageSize });
}

/**
 * 获取兑换商品列表（分页）
 */
function getExchangeItems(page, pageSize) {
  return get('/v1/points/exchange/items', { page, pageSize });
}

/**
 * 兑换商品
 */
function exchangeItem(itemId) {
  return post(`/v1/points/exchange/${itemId}`, {});
}

/**
 * 获取用户兑换记录（分页）
 */
function getExchangeRecords(page, pageSize) {
  return get('/v1/points/exchange/records', { page, pageSize });
}

module.exports = {
  getBalance,
  getTransactions,
  getExchangeItems,
  exchangeItem,
  getExchangeRecords
};
