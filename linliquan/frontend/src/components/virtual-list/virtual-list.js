// components/virtual-list/virtual-list.js
// 【P2性能优化】虚拟滚动列表组件
// 只渲染可视区域 + 上下缓冲区的列表项，大幅减少长列表的渲染节点数

Component({
  properties: {
    // 列表数据
    list: {
      type: Array,
      value: [],
      observer() {
        this.updateList();
      }
    },
    // 单项高度（px）
    itemHeight: {
      type: Number,
      value: 100
    },
    // 容器高度（px）
    height: {
      type: Number,
      value: 600
    },
    // 唯一标识字段名
    keyField: {
      type: String,
      value: 'id'
    }
  },

  data: {
    visibleItems: [],   // 当前可视项（含缓冲）
    startIndex: 0,      // 起始索引
    endIndex: 0,        // 结束索引
    totalHeight: 0,     // 列表总高度（占位撑开滚动条）
    offsetY: 0          // 当前滚动偏移
  },

  methods: {
    /**
     * 根据scrollTop计算需要渲染的可见项
     */
    computeVisible(scrollTop) {
      const { list, itemHeight, height } = this.properties;
      if (!list || list.length === 0) {
        this.setData({
          visibleItems: [],
          startIndex: 0,
          endIndex: 0,
          totalHeight: 0,
          offsetY: 0
        });
        return;
      }

      const totalHeight = list.length * itemHeight;

      // 计算可视范围起始索引
      let startIndex = Math.floor(scrollTop / itemHeight);
      if (startIndex < 0) startIndex = 0;

      // 可视区域内能容纳的项数
      const visibleCount = Math.ceil(height / itemHeight);

      // 上下各留 3 个缓冲项，避免滚动时白屏
      const buffer = 3;
      startIndex = Math.max(0, startIndex - buffer);

      let endIndex = startIndex + visibleCount + buffer * 2;
      if (endIndex > list.length) endIndex = list.length;

      // 截取可视项并补充绝对定位所需的 _top
      const visibleItems = list.slice(startIndex, endIndex).map((item, i) => {
        return Object.assign({}, item, {
          _top: (startIndex + i) * itemHeight
        });
      });

      this.setData({
        visibleItems,
        startIndex,
        endIndex,
        totalHeight,
        offsetY: scrollTop
      });
    },

    /**
     * 滚动事件处理
     */
    onScroll(e) {
      const scrollTop = e.detail.scrollTop;
      this.computeVisible(scrollTop);
    },

    /**
     * 列表变更时重新计算
     */
    updateList() {
      this.computeVisible(this.data.offsetY || 0);
    }
  },

  lifetimes: {
    attached() {
      this.updateList();
    }
  }
});
