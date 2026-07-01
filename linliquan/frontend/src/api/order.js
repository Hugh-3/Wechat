const { get, post } = require('../utils/request');

exports.getNearbyOrders = (lat, lng, radius) =>
  get('/v1/orders/nearby', { lat, lng, radius });

exports.createOrder = (data) => post('/v1/orders', data);

exports.acceptOrder = (id) => post(`/v1/orders/${id}/accept`, {});
