const { get, post } = require('../utils/request');

function getList(type, page, pageSize) {
  return get('/v1/posts', { type, page, pageSize });
}

function getDetail(id) {
  return get(`/v1/posts/${id}`);
}

function create(data) {
  return post('/v1/posts', data);
}

function toggleLike(id, liked) {
  return post(`/v1/posts/${id}/like`, { liked });
}

function getNearby(lat, lng, radius, page, pageSize) {
  return get('/v1/posts/nearby', { lat, lng, radius, page, pageSize });
}

module.exports = {
  getList,
  getDetail,
  create,
  toggleLike,
  getNearby
};
