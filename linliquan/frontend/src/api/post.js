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

function getMyPosts(page, pageSize) {
  return get('/v1/posts/my', { page, pageSize });
}

function search(keyword, type, page, pageSize) {
  return get('/v1/posts/search', { keyword, type, page, pageSize });
}

module.exports = {
  getList,
  getDetail,
  create,
  toggleLike,
  getNearby,
  getMyPosts,
  search
};
