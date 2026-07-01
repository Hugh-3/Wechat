/**
 * 图片处理工具
 * 【P2性能优化】
 * - 服务端图片通过腾讯云COS imageMogr2参数按需返回缩略/中等尺寸
 * - 客户端本地图片使用 wx.compressImage 压缩后再上传
 */

// 缩略图尺寸
const THUMBNAIL_WIDTH = 200;
const THUMBNAIL_HEIGHT = 200;

// 中等尺寸（列表展示）
const MEDIUM_WIDTH = 600;
const MEDIUM_HEIGHT = 600;

/**
 * 拼接图片处理参数URL
 * 追加腾讯云COS imageMogr2/thumbnail/{width}x{height} 缩略参数
 * @param {string} url 原图URL
 * @param {number} width 目标宽度
 * @param {number} height 目标高度
 * @returns {string} 处理后URL
 */
const getCompressedUrl = (url, width, height) => {
  if (!url || typeof url !== 'string') return url;
  // 仅对http(s)网络图片追加处理参数，本地临时文件路径不处理
  if (!/^https?:\/\//i.test(url)) return url;
  const separator = url.indexOf('?') >= 0 ? '&' : '?';
  return url + separator + 'imageMogr2/thumbnail/' + width + 'x' + height;
};

/**
 * 获取缩略图URL（200x200）
 * 适用于：列表头像、九宫格缩略图
 * @param {string} url 原图URL
 * @returns {string}
 */
const getThumbnailUrl = (url) => {
  return getCompressedUrl(url, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
};

/**
 * 获取中等尺寸图片URL（600x600）
 * 适用于：信息流列表大图展示
 * @param {string} url 原图URL
 * @returns {string}
 */
const getMediumUrl = (url) => {
  return getCompressedUrl(url, MEDIUM_WIDTH, MEDIUM_HEIGHT);
};

/**
 * 压缩本地图片文件
 * 使用 wx.compressImage API，返回压缩后的临时文件路径
 * @param {string} filePath 图片临时文件路径
 * @param {number} quality 压缩质量 0-100，默认60
 * @returns {Promise<string>} 压缩后的临时文件路径
 */
const compressImage = (filePath, quality = 60) => {
  return new Promise((resolve, reject) => {
    if (!filePath) {
      resolve(filePath);
      return;
    }
    wx.compressImage({
      src: filePath,
      quality: quality,
      success: (res) => {
        resolve(res.tempFilePath);
      },
      fail: (err) => {
        // 压缩失败则回退使用原图
        console.warn('图片压缩失败，使用原图', err);
        resolve(filePath);
      }
    });
  });
};

/**
 * 选择并压缩多张图片
 * @param {number} count 最多选择图片数量，默认9
 * @returns {Promise<Array<string>>} 压缩后的临时文件路径数组
 */
const chooseAndCompressImages = (count = 9) => {
  return new Promise((resolve, reject) => {
    wx.chooseImage({
      count: count,
      success: async (res) => {
        const tempFiles = res.tempFilePaths || [];
        try {
          // 逐张压缩
          const compressedPaths = [];
          for (let i = 0; i < tempFiles.length; i++) {
            const compressedPath = await compressImage(tempFiles[i], 60);
            compressedPaths.push(compressedPath);
          }
          resolve(compressedPaths);
        } catch (err) {
          reject(err);
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
};

module.exports = {
  getCompressedUrl,
  getThumbnailUrl,
  getMediumUrl,
  compressImage,
  chooseAndCompressImages
};
