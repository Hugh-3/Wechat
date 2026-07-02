/**
 * Mock数据生成器
 * 用于前端并行开发，不依赖后端接口
 */

// 模拟点赞状态
const likeStatus = {};

// 模拟评论数据
const mockComments = {
  1: [
    {
      id: 101,
      userId: 200,
      userName: '赵叔叔',
      userAvatar: '/assets/avatar4.png',
      content: '真的吗？下班我去看看，正好想买水果',
      createdAt: '2026-07-01 11:00',
      likeCount: 3
    },
    {
      id: 102,
      userId: 201,
      userName: '刘阿姨',
      userAvatar: '/assets/avatar5.png',
      content: '我昨天也买了，确实新鲜，老板人也很好',
      createdAt: '2026-07-01 10:45',
      likeCount: 5
    }
  ],
  2: [
    {
      id: 103,
      userId: 202,
      userName: '陈先生',
      userAvatar: '/assets/avatar6.png',
      content: '我要一个！怎么联系你？',
      createdAt: '2026-07-01 09:30',
      likeCount: 1
    }
  ]
};

// 模拟帖子数据
const mockPosts = [
  {
    id: 1,
    userId: 100,
    userName: '李阿姨',
    userAvatar: '/assets/avatar1.png',
    postType: 1, // 信息广场
    title: '好消息！楼下新开了一家水果店',
    content: '今天发现小区西门新开了一家水果店，价格比超市便宜不少，苹果才3块钱一斤，老板是咱小区业主，值得信赖！',
    images: ['/assets/fruit1.jpg'],
    distance: '约200米',
    likeCount: 45,
    commentCount: 12,
    viewCount: 328,
    createdAt: '2026-07-01 10:30'
  },
  {
    id: 2,
    userId: 101,
    userName: '张先生',
    userAvatar: '/assets/avatar2.png',
    postType: 2, // 邻里互助
    title: '拼单！西瓜2元一斤，差2人',
    content: '团购西瓜，基地直发，2元一斤，每个至少10斤起。有要一起拼的吗？明天下午到货。',
    images: ['/assets/watermelon.jpg'],
    distance: '约500米',
    likeCount: 23,
    commentCount: 8,
    createdAt: '2026-07-01 09:15'
  },
  {
    id: 3,
    userId: 102,
    userName: '王奶奶',
    userAvatar: '/assets/avatar3.png',
    postType: 2, // 邻里互助
    title: '求助：谁家有梯子借用一下',
    content: '家里灯坏了要换，需要借用一下人字梯，有的话请联系，谢谢！',
    images: [],
    distance: '约800米',
    likeCount: 5,
    commentCount: 3,
    createdAt: '2026-07-01 08:00'
  }
];

// 获取帖子列表
const getPostList = (type, page = 1, pageSize = 10) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const filtered = type ? mockPosts.filter(p => p.postType === type) : mockPosts;
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      resolve({
        list: filtered.slice(start, end),
        total: filtered.length,
        page,
        pageSize
      });
    }, 300);
  });
};

// 获取互助附近列表（按距离排序）
const getNearbyOrders = (lat, lng, radiusKm = 5) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const orders = mockPosts
        .filter(p => p.postType === 2)
        .sort((a, b) => {
          const distA = parseInt(a.distance.match(/\d+/)[0]);
          const distB = parseInt(b.distance.match(/\d+/)[0]);
          return distA - distB;
        });
      resolve({ list: orders, total: orders.length });
    }, 300);
  });
};

// 模拟发布动态
const createPost = (data) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      // 模拟验证状态检查
      const status = wx.getStorageSync('mockUserStatus') || 0;
      if (status !== 2) {
        reject({ code: 40301, message: '仅认证业主可发布内容' });
        return;
      }
      const newPost = {
        id: Date.now(),
        ...data,
        likeCount: 0,
        commentCount: 0,
        viewCount: 0,
        createdAt: new Date().toISOString().slice(0, 16).replace('T', ' ')
      };
      mockPosts.unshift(newPost);
      resolve(newPost);
    }, 500);
  });
};

// 点赞/取消点赞
const toggleLike = (postId, liked) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      likeStatus[postId] = liked;
      const post = mockPosts.find(p => p.id === postId);
      if (post) {
        post.likeCount = liked ? post.likeCount + 1 : Math.max(0, post.likeCount - 1);
      }
      resolve({ success: true });
    }, 200);
  });
};

// 获取帖子详情
const getPostDetail = (postId) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const post = mockPosts.find(p => p.id == postId);
      if (post) {
        resolve({
          ...post,
          liked: !!likeStatus[postId]
        });
      } else {
        reject({ message: '帖子不存在' });
      }
    }, 300);
  });
};

// 获取评论列表
const getComments = (postId, page = 1, pageSize = 20) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const comments = mockComments[postId] || [];
      resolve({
        list: comments,
        total: comments.length,
        page,
        pageSize
      });
    }, 300);
  });
};

// 发表评论
const createComment = (postId, content) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const status = wx.getStorageSync('mockUserStatus') || 0;
      if (status !== 2) {
        reject({ code: 40301, message: '仅认证业主可发表评论' });
        return;
      }
      const newComment = {
        id: Date.now(),
        userId: 999,
        userName: '我',
        userAvatar: '/assets/my-avatar.png',
        content,
        createdAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
        likeCount: 0
      };
      if (!mockComments[postId]) {
        mockComments[postId] = [];
      }
      mockComments[postId].unshift(newComment);
      const post = mockPosts.find(p => p.id == postId);
      if (post) {
        post.commentCount = (post.commentCount || 0) + 1;
      }
      resolve(newComment);
    }, 500);
  });
};

function getCurrentUserStatus() {
  try {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo && userInfo.verificationStatus !== undefined) {
      return userInfo.verificationStatus;
    }
  } catch (e) {}
  return 0;
}

function isVerified() {
  return getCurrentUserStatus() === 2;
}

function handleMock(url, method, data) {
  method = method.toUpperCase();

  if (url === '/v1/auth/login' && method === 'POST') {
    const user = {
      id: 1,
      nickname: '测试用户',
      avatarUrl: '/assets/avatar1.png',
      verificationStatus: 0,
      verificationDesc: '未认证'
    };
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        user,
        accessToken: 'mock_token_UNAUTH_' + Date.now()
      }
    };
  }

  if (url === '/v1/auth/login-mock' && method === 'POST') {
    const statusType = data && data.statusType !== undefined ? data.statusType : 0;
    const statusMap = {
      0: { status: 0, desc: '未认证', token: 'mock_token_UNAUTH' },
      1: { status: 1, desc: '认证中', token: 'mock_token_PENDING' },
      2: { status: 2, desc: '已认证', token: 'mock_token_VERIFIED' }
    };
    const info = statusMap[statusType] || statusMap[0];
    const user = {
      id: 1,
      nickname: '测试用户',
      avatarUrl: '/assets/avatar1.png',
      verificationStatus: info.status,
      verificationDesc: info.desc
    };
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        user,
        accessToken: info.token + '_' + Date.now()
      }
    };
  }

  if (url === '/v1/auth/status' && method === 'GET') {
    const status = getCurrentUserStatus();
    const statusDesc = ['未认证', '认证中', '已认证'][status] || '未认证';
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        verificationStatus: status,
        description: statusDesc,
        canWrite: status === 2,
        canRead: true
      }
    };
  }

  if (url === '/v1/auth/apply-verification' && method === 'POST') {
    if (!isVerified()) {
      const userInfo = wx.getStorageSync('userInfo') || {};
      userInfo.verificationStatus = 1;
      userInfo.verificationDesc = '认证中';
      wx.setStorageSync('userInfo', userInfo);
    }
    return {
      success: true,
      code: 200,
      message: '提交成功，等待审核'
    };
  }

  if (url.match(/^\/v1\/posts\/?$/) && method === 'GET') {
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        list: mockPosts,
        total: mockPosts.length,
        page: data && data.page ? data.page : 1,
        pageSize: data && data.pageSize ? data.pageSize : 10
      }
    };
  }

  if (url.match(/^\/v1\/posts\/?$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      const errCode = status === 0 ? 40301 : 40302;
      const errMsg = status === 0 ? '仅认证业主可进行此操作' : '您的业主认证正在审核中，审核通过后即可使用';
      return { success: false, code: errCode, message: errMsg };
    }
    const newPost = {
      id: Date.now(),
      userId: 999,
      userName: '我',
      userAvatar: '/assets/my-avatar.png',
      postType: data && data.postType ? data.postType : 1,
      title: data && data.title ? data.title : '',
      content: data && data.content ? data.content : '',
      images: [],
      likeCount: 0,
      commentCount: 0,
      viewCount: 0,
      createdAt: new Date().toISOString().slice(0, 16).replace('T', ' ')
    };
    mockPosts.unshift(newPost);
    return {
      success: true,
      code: 200,
      message: '发布成功',
      data: newPost
    };
  }

  if (url.match(/^\/v1\/posts\/my/) && method === 'GET') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      return { success: false, code: 40301, message: '仅认证业主可进行此操作' };
    }
    const myPosts = mockPosts.filter(p => p.userId === 999 || p.userId === 100);
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        list: myPosts,
        total: myPosts.length,
        page: 1,
        pageSize: 10
      }
    };
  }

  if (url.match(/^\/v1\/posts\/nearby/) && method === 'GET') {
    const helperPosts = mockPosts.filter(p => p.postType === 2 || p.helpType);
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        list: helperPosts.map(p => ({
          ...p,
          distance: Math.floor(Math.random() * 5000) + 100
        })),
        total: helperPosts.length
      }
    };
  }

  if (url.match(/^\/v1\/posts\/\d+\/?$/) && method === 'GET') {
    const id = parseInt(url.split('/')[3]);
    const post = mockPosts.find(p => p.id === id);
    if (post) {
      return { success: true, code: 200, message: '操作成功', data: post };
    }
    return { success: false, code: 404, message: '帖子不存在' };
  }

  if (url.match(/^\/v1\/posts\/\d+\/like$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      const errCode = status === 0 ? 40301 : 40302;
      const errMsg = status === 0 ? '仅认证业主可进行此操作' : '您的业主认证正在审核中，审核通过后即可使用';
      return { success: false, code: errCode, message: errMsg };
    }
    const id = parseInt(url.split('/')[3]);
    const post = mockPosts.find(p => p.id === id);
    if (post) {
      const liked = data && data.liked;
      if (liked) {
        post.likeCount = (post.likeCount || 0) + 1;
        likeStatus[id] = true;
      } else {
        post.likeCount = Math.max(0, (post.likeCount || 0) - 1);
        delete likeStatus[id];
      }
      return { success: true, code: 200, message: '操作成功', data: { liked: !!liked, likeCount: post.likeCount } };
    }
    return { success: false, code: 404, message: '帖子不存在' };
  }

  if (url.match(/^\/v1\/comments\/?$/) && method === 'GET') {
    const postId = data && data.postId ? data.postId : 1;
    const comments = mockComments[postId] || [];
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        list: comments,
        total: comments.length
      }
    };
  }

  if (url.match(/^\/v1\/comments\/?$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      const errCode = status === 0 ? 40301 : 40302;
      const errMsg = status === 0 ? '仅认证业主可进行此操作' : '您的业主认证正在审核中，审核通过后即可使用';
      return { success: false, code: errCode, message: errMsg };
    }
    const postId = data && data.postId ? data.postId : 1;
    const content = data && data.content ? data.content : '';
    const newComment = {
      id: Date.now(),
      userId: 999,
      userName: '我',
      userAvatar: '/assets/my-avatar.png',
      content,
      likeCount: 0,
      createdAt: new Date().toISOString().slice(0, 16).replace('T', ' ')
    };
    if (!mockComments[postId]) {
      mockComments[postId] = [];
    }
    mockComments[postId].unshift(newComment);
    const post = mockPosts.find(p => p.id == postId);
    if (post) {
      post.commentCount = (post.commentCount || 0) + 1;
    }
    return {
      success: true,
      code: 200,
      message: '评论成功',
      data: newComment
    };
  }

  if (url.match(/^\/v1\/comments\/\d+\/?$/) && method === 'DELETE') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      return { success: false, code: 40301, message: '仅认证业主可进行此操作' };
    }
    return { success: true, code: 200, message: '删除成功' };
  }

  if (url.match(/^\/v1\/comments\/\d+\/like$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      return { success: false, code: 40301, message: '仅认证业主可进行此操作' };
    }
    return { success: true, code: 200, message: '操作成功' };
  }

  if (url.match(/^\/v1\/orders\/my/) && method === 'GET') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      return { success: false, code: 40301, message: '仅认证业主可进行此操作' };
    }
    const myOrders = mockPosts.filter(p => p.postType === 2).map(p => ({
      id: p.id,
      postId: p.id,
      userId: p.userId,
      userName: p.userName,
      userAvatar: p.userAvatar,
      helpType: p.helpType || 1,
      helpTypeName: ['', '拼单团购', '代取代买', '生活求助', '技能交换'][p.helpType || 1],
      rewardAmount: p.rewardAmount || 0,
      status: 1,
      statusText: '待接单',
      title: p.title,
      content: p.content,
      createdAt: p.createdAt,
      isOwner: true
    }));
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        list: myOrders,
        total: myOrders.length,
        page: 1,
        pageSize: 10
      }
    };
  }

  if (url.match(/^\/v1\/orders\/nearby/) && method === 'GET') {
    const orders = mockOrders || mockPosts.filter(p => p.postType === 2).map(p => ({
      id: p.id,
      postId: p.id,
      userId: p.userId,
      userName: p.userName,
      userAvatar: p.userAvatar,
      helpType: p.helpType || 1,
      rewardAmount: p.rewardAmount || 0,
      distance: Math.floor(Math.random() * 5000) + 100,
      status: 1,
      title: p.title,
      content: p.content,
      createdAt: p.createdAt
    }));
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        list: orders,
        total: orders.length
      }
    };
  }

  if (url.match(/^\/v1\/orders\/?$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      const errCode = status === 0 ? 40301 : 40302;
      const errMsg = status === 0 ? '仅认证业主可进行此操作' : '您的业主认证正在审核中，审核通过后即可使用';
      return { success: false, code: errCode, message: errMsg };
    }
    return {
      success: true,
      code: 200,
      message: '发布成功',
      data: { id: Date.now(), status: 1 }
    };
  }

  if (url.match(/^\/v1\/orders\/\d+\/accept$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      const errCode = status === 0 ? 40301 : 40302;
      const errMsg = status === 0 ? '仅认证业主可进行此操作' : '您的业主认证正在审核中，审核通过后即可使用';
      return { success: false, code: errCode, message: errMsg };
    }
    return { success: true, code: 200, message: '接单成功' };
  }

  if (url.match(/^\/v1\/orders\/\d+\/complete$/) && method === 'POST') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      const errCode = status === 0 ? 40301 : 40302;
      const errMsg = status === 0 ? '仅认证业主可进行此操作' : '您的业主认证正在审核中，审核通过后即可使用';
      return { success: false, code: errCode, message: errMsg };
    }
    return { success: true, code: 200, message: '完成成功' };
  }

  if (url.match(/^\/v1\/orders\/\d+\/?$/) && method === 'GET') {
    const id = parseInt(url.split('/')[3]);
    const post = mockPosts.find(p => p.id === id);
    if (post) {
      const order = {
        id: post.id,
        postId: post.id,
        userId: post.userId,
        userName: post.userName,
        userAvatar: post.userAvatar,
        helpType: post.helpType || 1,
        helpTypeName: ['', '拼单团购', '代取代买', '生活求助', '技能交换'][post.helpType || 1],
        rewardAmount: post.rewardAmount || 0,
        distance: Math.floor(Math.random() * 5000) + 100,
        status: 1,
        statusText: '待接单',
        title: post.title,
        content: post.content,
        images: post.images || [],
        createdAt: post.createdAt
      };
      return { success: true, code: 200, message: '操作成功', data: order };
    }
    return { success: false, code: 404, message: '订单不存在' };
  }

  // 收藏/取消收藏
  if (url.match(/^\/v1\/favorites\/(\d+)$/) && method === 'POST') {
    const postId = parseInt(url.match(/^\/v1\/favorites\/(\d+)$/)[1]);
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: { favorited: data.favorited, favoriteCount: Math.floor(Math.random() * 50) + 1 }
    };
  }

  // 我的收藏列表
  if (url.match(/^\/v1\/favorites\/my/) && method === 'GET') {
    const status = getCurrentUserStatus();
    if (status !== 2) {
      return { success: false, code: 40301, message: '仅认证业主可进行此操作' };
    }
    // 返回前3条帖子作为收藏
    const favPosts = mockPosts.slice(0, 3).map(p => ({
      ...p,
      favoritedAt: '2026-07-02 09:00'
    }));
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: { list: favPosts, total: favPosts.length, page: 1, pageSize: 10 }
    };
  }

  // 用户统计数据
  if (url.match(/^\/v1\/users\/stats/) && method === 'GET') {
    return {
      success: true,
      code: 200,
      message: '操作成功',
      data: {
        postCount: 8,
        likeCount: 56,
        commentCount: 23,
        favoriteCount: 5,
        orderCount: 4
      }
    };
  }

  return {
    success: false,
    code: 404,
    message: 'Mock接口未找到: ' + method + ' ' + url
  };
}

module.exports = {
  mockPosts,
  mockComments,
  likeStatus,
  getPostList,
  getNearbyOrders,
  createPost,
  toggleLike,
  getPostDetail,
  getComments,
  createComment,
  handleMock
};
