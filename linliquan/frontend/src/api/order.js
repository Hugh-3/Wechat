const { get, post } = require('../utils/request');

function getNearby(lat, lng, radius) {
  return get('/v1/orders/nearby', { lat, lng, radius });
}

function getDetail(id) {
  return get(`/v1/orders/${id}`);
}

function create(data) {
  return post('/v1/orders', data);
}

function accept(id) {
  return post(`/v1/orders/${id}/accept`, {});
}

function complete(id) {
  return post(`/v1/orders/${id}/complete`, {});
}

module.exports = {
  getNearby,
  getDetail,
  create,
  accept,
  complete
};
