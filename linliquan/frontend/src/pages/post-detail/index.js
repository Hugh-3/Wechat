// pages/post-detail/index.js
// 帖子详情页

const { getCurrentUserStatus, canWrite } = require('../../utils/auth');
const { getPostDetail, getComments, createComment, toggleLike } = require('../../mock/data');
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
    userStatus: 0
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

  // 加载帖子详情
  async loadPostDetail() {
    try {
      const post = await getPostDetail(this.data.postId);
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

  // 加载评论
  async loadComments() {
    this.setData({ loading: true });
    try {
      const result = await getComments(this.data.postId);
      const comments = result.list.map(item => ({
        ...item,
        timeText: formatRelativeTime(item.createdAt),
        liked: false
      }));
      this.setData({
        comments,
        commentCount: result.total
      });
    } catch (err) {
      console.error('加载评论失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  // 评论输入
  onCommentInput(e) {
    this.setData({ commentText: e.detail.value });
  },

  // 提交评论
  async submitComment() {
    if (!this.data.canComment) {
      this.showAuthModal();
      return;
    }

    const content = this.data.commentText.trim();
    if (!content) return;

    this.setData({ submitting: true });

    try {
      const newComment = await createComment(this.data.postId, content);
      newComment.timeText = formatRelativeTime(newComment.createdAt);
      newComment.liked = false;

      this.setData({
        comments: [newComment, ...this.data.comments],
        commentCount: this.data.commentCount + 1,
        commentText: '',
        post: {
          ...this.data.post,
          commentCount: (this.data.post.commentCount || 0) + 1
        }
      });

      wx.showToast({ title: '评论成功', icon: 'success' });
    } catch (err) {
      console.error('评论失败', err);
      wx.showToast({ title: err.message || '评论失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  // 点赞帖子
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

    toggleLike(this.data.postId, newLiked).catch(() => {
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

  // 点赞评论
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
  },

  // 回复
  onReply(e) {
    if (!this.data.canComment) {
      this.showAuthModal();
      return;
    }
    const name = e.currentTarget.dataset.name;
    wx.showToast({
      title: `回复 ${name}`,
      icon: 'none'
    });
  },

  // 预览图片
  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.post.images;
    wx.previewImage({
      current: images[index],
      urls: images
    });
  },

  // 显示认证弹窗
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
