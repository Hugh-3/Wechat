package com.linliquan.util;

/**
 * 图片压缩工具类
 * 【P2性能优化】生成CDN图片处理参数URL，按需返回不同尺寸的图片
 *
 * 适配腾讯云COS数据处理：通过在URL后追加 imageMogr2/thumbnail/{width}x{height} 参数
 * 由CDN边缘节点完成图片缩放，减少源站带宽与客户端流量消耗。
 */
public class ImageCompressionUtil {

    // 缩略图尺寸
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    // 中等尺寸（列表展示）
    private static final int MEDIUM_WIDTH = 600;
    private static final int MEDIUM_HEIGHT = 600;

    private ImageCompressionUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取指定宽高的压缩图片URL
     * 通过腾讯云COS imageMogr2 缩略参数实现按需压缩
     *
     * @param originalUrl 原图URL
     * @param width       目标宽度（像素）
     * @param height      目标高度（像素）
     * @return 追加压缩参数后的URL
     */
    public static String getCompressedImageUrl(String originalUrl, int width, int height) {
        if (originalUrl == null || originalUrl.isEmpty()) {
            return originalUrl;
        }
        if (!isValidImageUrl(originalUrl)) {
            return originalUrl;
        }
        String separator = originalUrl.contains("?") ? "&" : "?";
        return originalUrl + separator + "imageMogr2/thumbnail/" + width + "x" + height;
    }

    /**
     * 获取缩略图URL（200x200）
     * 适用于：列表头像、九宫格缩略图
     *
     * @param originalUrl 原图URL
     * @return 缩略图URL
     */
    public static String getThumbnailUrl(String originalUrl) {
        return getCompressedImageUrl(originalUrl, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
    }

    /**
     * 获取中等尺寸图片URL（600x600）
     * 适用于：信息流列表大图展示
     *
     * @param originalUrl 原图URL
     * @return 中等尺寸图片URL
     */
    public static String getMediumUrl(String originalUrl) {
        return getCompressedImageUrl(originalUrl, MEDIUM_WIDTH, MEDIUM_HEIGHT);
    }

    /**
     * 获取原图URL（不做压缩）
     *
     * @param originalUrl 原图URL
     * @return 原图URL
     */
    public static String getOriginalUrl(String originalUrl) {
        return originalUrl;
    }

    /**
     * 校验图片URL格式是否合法
     * - 非空
     * - http/https协议
     * - 以常见图片后缀结尾（或URL含查询参数）
     *
     * @param url 待校验URL
     * @return true合法，false非法
     */
    public static boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lower = url.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        // 去掉查询串和锚点后判断后缀
        String path = lower.split("[?#]")[0];
        return path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".png")
                || path.endsWith(".webp")
                || path.endsWith(".gif")
                || path.endsWith(".bmp");
    }
}
