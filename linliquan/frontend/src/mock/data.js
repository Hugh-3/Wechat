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
export const getPostList = (type, page = 1, pageSize = 10) => {
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
export const getNearbyOrders = (lat, lng, radiusKm = 5) => {
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
export const createPost = (data) => {
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
export const toggleLike = (postId, liked) => {
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
export const getPostDetail = (postId) => {
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
export const getComments = (postId, page = 1, pageSize = 20) => {
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
export const createComment = (postId, content) => {
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
