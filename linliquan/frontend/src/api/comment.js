const { get, post, del } = require('../utils/request');

exports.getCommentList = (postId, page, pageSize) =>
  get('/v1/comments', { postId, page, pageSize });

exports.createComment = (data) => post('/v1/comments', data);

exports.deleteComment = (id) => del(`/v1/comments/${id}`);

exports.likeComment = (id) => post(`/v1/comments/${id}/like`, {});
