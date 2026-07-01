// components/report-dialog/report-dialog.js
// 可复用举报弹窗组件
// 用法：<report-dialog show="{{show}}" targetType="{{targetType}}" targetId="{{targetId}}" bind:close="..." bind:submitted="..."/>

const reportApi = require('../../api/report');

Component({
  properties: {
    // 是否显示
    show: {
      type: Boolean,
      value: false
    },
    // 举报目标类型：1-帖子, 2-评论, 3-用户
    targetType: {
      type: Number,
      value: 1
    },
    // 目标ID
    targetId: {
      type: Number,
      value: 0
    }
  },

  data: {
    // 举报原因选项
    reasonOptions: [
      { value: 1, label: '垃圾广告' },
      { value: 2, label: '违法违规' },
      { value: 3, label: '色情低俗' },
      { value: 4, label: '侮辱谩骂' },
      { value: 5, label: '其他' }
    ],
    selectedReason: 0,   // 选中的原因 value，0 表示未选
    description: '',      // 补充描述
    submitting: false     // 提交中
  },

  methods: {
    /**
     * 选择举报原因
     */
    onReasonSelect(e) {
      const value = Number(e.currentTarget.dataset.value);
      this.setData({ selectedReason: value });
    },

    /**
     * 输入补充描述
     */
    onDescriptionInput(e) {
      this.setData({ description: e.detail.value });
    },

    /**
     * 提交举报
     */
    async submit() {
      const { targetType, targetId } = this.properties;
      const { selectedReason, description, submitting } = this.data;

      if (submitting) return;

      if (!targetId) {
        wx.showToast({ title: '缺少举报目标', icon: 'none' });
        return;
      }
      if (!selectedReason) {
        wx.showToast({ title: '请选择举报原因', icon: 'none' });
        return;
      }

      this.setData({ submitting: true });
      try {
        await reportApi.createReport(targetType, targetId, selectedReason, description);
        wx.showToast({ title: '举报已提交', icon: 'success' });
        // 重置表单
        this.setData({ selectedReason: 0, description: '' });
        // 通知父组件已提交成功
        this.triggerEvent('submitted', { targetType, targetId, reason: selectedReason });
        // 关闭弹窗
        this.triggerEvent('close');
      } catch (err) {
        // request.js 已处理错误提示，这里仅兜底
        if (err && err.message && !err.code) {
          wx.showToast({ title: err.message, icon: 'none' });
        }
      } finally {
        this.setData({ submitting: false });
      }
    },

    /**
     * 关闭弹窗
     */
    close() {
      // 提交中时禁止关闭，避免误触
      if (this.data.submitting) return;
      this.setData({ selectedReason: 0, description: '' });
      this.triggerEvent('close');
    },

    /**
     * 阻止内容区点击事件冒泡到遮罩
     */
    noop() {}
  }
});
