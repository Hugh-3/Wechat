const { get, post, del } = require('../utils/request');

exports.getPostList = (type, page, pageSize) =>
  get('/v1/posts', { type, page, pageSize });

exports.getPostDetail = (id) => get(`/v1/posts/${id}`);

exports.createPost = (data) => post('/v1/posts', data);

exports.toggleLike = (id, liked) => post(`/v1/posts/${id}/like`, { liked });

exports.getNearbyOrders = (lat, lng, radius, page, pageSize) =>
  get('/v1/posts/nearby', { lat, lng, radius, page, pageSize });
