const { get, post, del } = require('../utils/request');

function getList(postId, page, pageSize) {
  return get('/v1/comments', { postId, page, pageSize });
}

function create(postId, content) {
  return post('/v1/comments', { postId, content });
}

function deleteComment(id) {
  return del(`/v1/comments/${id}`);
}

function toggleLike(id, liked) {
  return post(`/v1/comments/${id}/like`, { liked });
}

module.exports = {
  getList,
  create,
  delete: deleteComment,
  toggleLike
};
