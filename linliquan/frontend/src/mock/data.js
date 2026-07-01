/**
 * Mock数据生成器
 * 用于前端并行开发，不依赖后端接口
 */

// 模拟帖子数据
export const mockPosts = [
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
      resolve({ id: Date.now(), ...data });
    }, 500);
  });
};
