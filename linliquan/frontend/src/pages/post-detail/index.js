// pages/post-detail/index.js
// 帖子详情页

const { getCurrentUserStatus, canWrite } = require('../../utils/auth');
const postApi = require('../../api/post');
const commentApi = require('../../api/comment');
const { formatRelativeTime } = require('../../utils/time');

Page({
  data: {
    postId: null,
    post: null,
    comments: [],
    commentCount: 0,
    commentText: '',
    loading: true,
    submitting: false,
    canComment: false,
    userStatus: 0,
    replyTo: null  // 回复目标 { id, name, userId }
  },

  onLoad(options) {
    const postId = options.id;
    this.setData({ postId });

    const status = getCurrentUserStatus();
    this.setData({
      userStatus: status,
      canComment: canWrite()
    });

    this.loadPostDetail();
    this.loadComments();
  },

  onShow() {
    const status = getCurrentUserStatus();
    this.setData({
      userStatus: status,
      canComment: canWrite()
    });
  },

  async loadPostDetail() {
    try {
      const res = await postApi.getDetail(this.data.postId);
      const post = res.data || {};
      post.timeText = formatRelativeTime(post.createdAt);
      this.setData({
        post,
        commentCount: post.commentCount || 0
      });
    } catch (err) {
      console.error('加载帖子详情失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  async loadComments() {
    this.setData({ loading: true });
    try {
      const res = await commentApi.getList(this.data.postId, 1, 20);
      const result = res.data || { list: [], total: 0 };
      const comments = (result.list || []).map(item => ({
        ...item,
        timeText: formatRelativeTime(item.createdAt),
        liked: false
      }));
      this.setData({
        comments,
        commentCount: result.total || 0
      });
    } catch (err) {
      console.error('加载评论失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  onCommentInput(e) {
    this.setData({ commentText: e.detail.value });
  },

  async submitComment() {
    if (!this.data.canComment) {
      this.showAuthModal();
      return;
    }

    const content = this.data.commentText.trim();
    if (!content) return;

    this.setData({ submitting: true });

    try {
      const replyTo = this.data.replyTo;
      const res = await commentApi.create(
        this.data.postId,
        content,
        replyTo ? replyTo.id : null,
        replyTo ? replyTo.userId : null
      );
      const newComment = res.data || {};
      newComment.timeText = formatRelativeTime(newComment.createdAt);
      newComment.liked = false;
      if (replyTo) {
        newComment.replyToUserName = replyTo.name;
      }

      this.setData({
        comments: [newComment, ...this.data.comments],
        commentCount: this.data.commentCount + 1,
        commentText: '',
        replyTo: null,
        post: {
          ...this.data.post,
          commentCount: (this.data.post.commentCount || 0) + 1
        }
      });

      wx.showToast({ title: replyTo ? '回复成功' : '评论成功', icon: 'success' });
    } catch (err) {
      console.error('评论失败', err);
      wx.showToast({ title: err.message || '评论失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  onPostLike() {
    if (!this.data.canComment) {
      this.showAuthModal();
      return;
    }
    const post = this.data.post;
    const newLiked = !post.liked;
    const newLikeCount = newLiked ? post.likeCount + 1 : post.likeCount - 1;

    this.setData({
      post: {
        ...post,
        liked: newLiked,
        likeCount: newLikeCount
      }
    });

    postApi.toggleLike(this.data.postId, newLiked).catch(() => {
      this.setData({
        post: {
          ...post,
          liked: post.liked,
          likeCount: post.likeCount
        }
      });
      wx.showToast({ title: '操作失败', icon: 'none' });
    });
  },

  onCommentLike(e) {
    if (!this.data.canComment) {
      this.showAuthModal();
      return;
    }
    const commentId = e.currentTarget.dataset.id;
    const comments = [...this.data.comments];
    const index = comments.findIndex(c => c.id === commentId);
    if (index === -1) return;

    const comment = comments[index];
    const newLiked = !comment.liked;
    const newLikeCount = newLiked ? (comment.likeCount || 0) + 1 : Math.max(0, (comment.likeCount || 0) - 1);

    comments[index] = {
      ...comment,
      liked: newLiked,
      likeCount: newLikeCount
    };

    this.setData({ comments });

    commentApi.toggleLike(commentId, newLiked).catch(() => {
      comments[index] = {
        ...comment,
        liked: comment.liked,
        likeCount: comment.likeCount
      };
      this.setData({ comments });
      wx.showToast({ title: '操作失败', icon: 'none' });
    });
  },

  onReply(e) {
    if (!this.data.canComment) {
      this.showAuthModal();
      return;
    }
    const { id, name, userid } = e.currentTarget.dataset;
    this.setData({
      replyTo: { id: id, name: name, userId: userid }
    });
  },

  // 取消回复
  onCancelReply() {
    this.setData({ replyTo: null });
  },

  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.post.images;
    wx.previewImage({
      current: images[index],
      urls: images
    });
  },

  showAuthModal() {
    wx.showModal({
      title: '需要业主认证',
      content: '认证业主后才能参与互动\n\n🔒 安全认证，保护邻里',
      confirmText: '去认证',
      cancelText: '稍后再说',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/auth/index' });
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: this.data.post ? this.data.post.title : '邻里圈',
      path: `/pages/post-detail/index?id=${this.data.postId}`
    };
  }
});
