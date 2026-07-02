// pages/search/index.js
// 搜索页面，搜索帖子并支持搜索历史

const postApi = require('../../api/post');
const { formatRelativeTime } = require('../../utils/time');

// Tab 配置：type 对应后端 type 参数（null=全部, 1=信息广场, 2=邻里互助）
const TABS = [
  { key: 'all', name: '全部', type: null },
  { key: 'info', name: '信息广场', type: 1 },
  { key: 'help', name: '邻里互助', type: 2 }
];

Page({
  data: {
    keyword: '',            // 搜索关键词
    type: null,             // 当前 Tab 类型
    results: [],            // 搜索结果列表
    page: 1,                // 当前页码
    pageSize: 10,           // 每页数量
    hasMore: true,          // 是否还有更多
    loading: false,         // 是否正在加载
    searchHistory: [],      // 搜索历史
    activeTab: 'all',       // 当前选中的 Tab
    hasSearched: false      // 是否已经执行过搜索
  },

  onLoad() {
    // 读取本地搜索历史
    const history = wx.getStorageSync('searchHistory') || [];
    this.setData({ searchHistory: history });
  },

  // 关键词输入
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  // 切换 Tab
  onTabChange(e) {
    const key = e.currentTarget.dataset.key;
    if (key === this.data.activeTab) return;
    const tab = TABS.find(t => t.key === key) || TABS[0];
    this.setData({ activeTab: key, type: tab.type });
    // 已经搜索过则按新类型重新触发搜索
    if (this.data.hasSearched) {
      this.doSearch();
    }
  },

  // 输入框回车触发搜索
  onConfirmSearch() {
    this.doSearch();
  },

  // 点击搜索按钮
  onSearchTap() {
    this.doSearch();
  },

  // 点击历史词直接搜索
  onHistoryTap(e) {
    const word = e.currentTarget.dataset.word;
    if (!word) return;
    this.setData({ keyword: word }, () => {
      this.doSearch();
    });
  },

  // 清空搜索历史
  onClearHistory() {
    if (this.data.searchHistory.length === 0) return;
    wx.showModal({
      title: '清空搜索历史',
      content: '确定要清空全部搜索历史吗？',
      confirmText: '清空',
      confirmColor: '#fa5151',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('searchHistory');
        this.setData({ searchHistory: [] });
      }
    });
  },

  // 执行搜索
  doSearch() {
    const keyword = (this.data.keyword || '').trim();
    if (!keyword) {
      wx.showToast({ title: '请输入关键词', icon: 'none' });
      return;
    }

    // 写入历史
    this.saveHistory(keyword);

    // 重置结果并查询
    this.setData({
      results: [],
      page: 1,
      hasMore: true,
      hasSearched: true
    }, () => {
      this.loadResults();
    });
  },

  // 保存搜索历史，去重并保留最多 10 条
  saveHistory(keyword) {
    let history = [...this.data.searchHistory];
    history = history.filter(w => w !== keyword);
    history.unshift(keyword);
    if (history.length > 10) {
      history = history.slice(0, 10);
    }
    wx.setStorageSync('searchHistory', history);
    this.setData({ searchHistory: history });
  },

  // 加载搜索结果
  async loadResults() {
    if (this.data.loading || !this.data.hasMore) return;

    this.setData({ loading: true });

    try {
      const res = await postApi.search(
        this.data.keyword,
        this.data.type,
        this.data.page,
        this.data.pageSize
      );
      const result = res.data || { list: [], total: 0 };
      const list = (result.list || []).map(item => ({
        ...item,
        timeText: formatRelativeTime(item.createdAt)
      }));

      this.setData({
        results: [...this.data.results, ...list],
        page: this.data.page + 1,
        hasMore: list.length >= this.data.pageSize
      });
    } catch (err) {
      console.error('搜索失败', err);
      wx.showToast({ title: '搜索失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 点击帖子跳转详情
  onPostTap(e) {
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/post-detail/index?id=${postId}` });
  },

  // 上拉加载更多
  onReachBottom() {
    if (this.data.hasSearched) {
      this.loadResults();
    }
  }
});
