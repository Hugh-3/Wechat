const { get, post, del } = require('../utils/request');

function getList(postId, page, pageSize) {
  return get('/v1/comments', { postId, page, pageSize });
}

function create(postId, content, parentId, replyToUserId) {
  const data = { postId, content };
  if (parentId) data.parentId = parentId;
  if (replyToUserId) data.replyToUserId = replyToUserId;
  return post('/v1/comments', data);
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
