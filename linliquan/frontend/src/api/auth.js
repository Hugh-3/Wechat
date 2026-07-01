const { get, post } = require('../utils/request');

function login(phone) {
  return post('/v1/auth/login', { phone });
}

function getStatus() {
  return get('/v1/auth/status');
}

function applyVerification(params) {
  const { idCard, houseNumber, phone } = params || {};
  return post('/v1/auth/apply-verification', { idCard, houseNumber, phone });
}

module.exports = {
  login,
  getStatus,
  applyVerification
};
