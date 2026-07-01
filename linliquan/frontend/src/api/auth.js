const { get, post } = require('../utils/request');

exports.login = (phone) => post('/v1/auth/login', { phone });

exports.loginMock = (statusType) => post('/v1/auth/login-mock', { statusType });

exports.applyVerification = (idCard, houseNumber, certificateUrl) =>
  post('/v1/auth/apply-verification', { idCard, houseNumber, certificateUrl });

exports.getStatus = () => get('/v1/auth/status');
