/**
 * 三种认证状态的UI差异演示组件
 *
 * 【红线强制】状态校验逻辑：
 * - UNAUTH(0): 仅可浏览，发布/评论等操作弹出全屏认证引导（无关闭按钮）
 * - PENDING(1): 仅可浏览，发布按钮灰色+审核中角标
 * - VERIFIED(2): 全功能开放
 */

// 状态配置
export const statusConfig = {
  UNAUTH: {
    status: 0,
    label: '未认证',
    color: '#999',
    canWrite: false,
    canRead: true,
    publishButton: {
      enabled: false,
      badge: null,
      tooltip: null
    }
  },
  PENDING: {
    status: 1,
    label: '认证中',
    color: '#f5a623',
    canWrite: false,
    canRead: true,
    publishButton: {
      enabled: false,
      badge: '审核中',
      badgeColor: '#f5a623',
      tooltip: '您的业主认证正在审核中，预计1-2个工作日完成'
    }
  },
  VERIFIED: {
    status: 2,
    label: '已认证',
    color: '#07c160',
    canWrite: true,
    canRead: true,
    publishButton: {
      enabled: true,
      badge: null,
      tooltip: null
    }
  }
};

// 获取当前状态配置
export const getStatusConfig = (status) => {
  if (status === 0) return statusConfig.UNAUTH;
  if (status === 1) return statusConfig.PENDING;
  return statusConfig.VERIFIED;
};

// 发布按钮组件属性计算
export const computePublishButtonProps = (userStatus) => {
  const config = getStatusConfig(userStatus);

  return {
    disabled: !config.canWrite,
    badge: config.publishButton.badge,
    badgeColor: config.publishButton.badgeColor,
    tooltip: config.publishButton.tooltip,
    backgroundColor: config.canWrite ? '#07c160' : '#ccc',
    iconColor: '#fff'
  };
};

// 认证引导弹窗内容
export const getAuthGuideContent = () => ({
  title: '为了保护您和邻居的安全',
  subtitle: '只有认证业主才能使用此功能',
  features: [
    { icon: '🔒', text: '您的证件信息将加密存储' },
    { icon: '📋', text: '审核通过后7天自动删除原图' },
    { icon: '✅', text: '认证通过即可发布/互动' }
  ],
  primaryButton: '去认证',
  secondaryButton: '稍后再说', // 放在底部灰色小字，不显眼
  showCloseButton: false // 【红线强制】无关闭按钮
});

// 列表项权限处理
export const computeListItemActions = (userStatus) => {
  const config = getStatusConfig(userStatus);

  return {
    showContact: config.canWrite, // VERIFIED才显示联系方式
    showContactText: config.canWrite ? '我要联系' : '查看详情',
    contactTextHidden: !config.canWrite,
    distanceAccuracy: config.canWrite ? '精确' : '模糊', // 非认证用户显示模糊距离
    likeEnabled: config.canWrite,
    commentEnabled: config.canWrite
  };
};

// 模糊距离处理
export const getFuzzyDistance = (distance) => {
  const num = parseInt(distance.match(/\d+/)[0]);
  if (num < 300) return '约500米内';
  if (num < 800) return '约1公里内';
  if (num < 3000) return '约3公里内';
  return '约5公里内';
};
