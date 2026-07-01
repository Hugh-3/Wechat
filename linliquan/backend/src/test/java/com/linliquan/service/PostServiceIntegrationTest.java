package com.linliquan.service;

import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PostService集成测试
 * 测试帖子发布、列表查询、附近任务、点赞功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService集成测试")
class PostServiceIntegrationTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private PostService postService;

    private User verifiedUser;
    private User pendingUser;
    private User unauthUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 已认证用户
        verifiedUser = new User();
        verifiedUser.setId(1L);
        verifiedUser.setNickname("已认证用户");
        verifiedUser.setVerificationStatus(VerificationStatus.VERIFIED);

        // 认证中用户
        pendingUser = new User();
        pendingUser.setId(2L);
        pendingUser.setNickname("认证中用户");
        pendingUser.setVerificationStatus(VerificationStatus.PENDING);

        // 未认证用户
        unauthUser = new User();
        unauthUser.setId(3L);
        unauthUser.setNickname("未认证用户");
        unauthUser.setVerificationStatus(VerificationStatus.UNAUTH);

        // 测试帖子
        testPost = createTestPost(1L, 1L, 1, "测试帖子", "测试内容");
    }

    // ==================== 发布帖子测试 ====================

    @Test
    @DisplayName("集成测试 - 已认证用户发布信息广场帖子")
    void testCreatePost_Square_Success() {
        // 准备参数
        Map<String, Object> params = new HashMap<>();
        params.put("title", "新帖子");
        params.put("content", "帖子内容");
        params.put("type", 1);

        // Repository保存返回
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // 执行
        var result = postService.createPost(verifiedUser, params);

        // 验证
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("新帖子", result.getData().getTitle());
        verify(postRepository, times(1)).save(any(Post.class));
        verify(cacheService, times(1)).invalidatePostListCache(eq("1"), isNull());
    }

    @Test
    @DisplayName("集成测试 - 已认证用户发布互助帖子（含位置）")
    void testCreatePost_Help_WithLocation() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "寻求帮助");
        params.put("content", "需要帮忙取快递");
        params.put("type", 2);
        params.put("latitude", 39.9042);
        params.put("longitude", 116.4074);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        var result = postService.createPost(verifiedUser, params);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().getPostType());
        assertNotNull(result.getData().getLatitude());
        assertNotNull(result.getData().getLongitude());
    }

    @Test
    @DisplayName("集成测试 - 未认证用户发布帖子被拒绝")
    void testCreatePost_UnauthUser_Forbidden() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "新帖子");
        params.put("content", "内容");
        params.put("type", 1);

        var result = postService.createPost(unauthUser, params);

        assertFalse(result.isSuccess());
        assertEquals(40301, result.getCode());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("集成测试 - 认证中用户发布帖子被拒绝")
    void testCreatePost_PendingUser_Forbidden() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "新帖子");
        params.put("content", "内容");
        params.put("type", 1);

        var result = postService.createPost(pendingUser, params);

        assertFalse(result.isSuccess());
        assertEquals(40302, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 互助帖子缺少位置信息")
    void testCreatePost_Help_MissingLocation() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "寻求帮助");
        params.put("content", "需要帮忙");
        params.put("type", 2);
        // 缺少 latitude 和 longitude

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 标题为空")
    void testCreatePost_EmptyTitle() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "");
        params.put("content", "内容");
        params.put("type", 1);

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 标题超长")
    void testCreatePost_TitleTooLong() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "a".repeat(101)); // 超过100字符
        params.put("content", "内容");
        params.put("type", 1);

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 内容超长")
    void testCreatePost_ContentTooLong() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "标题");
        params.put("content", "a".repeat(2001)); // 超过2000字符
        params.put("type", 1);

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 无效帖子类型")
    void testCreatePost_InvalidPostType() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "标题");
        params.put("content", "内容");
        params.put("type", 3); // 无效类型

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 无效纬度")
    void testCreatePost_InvalidLatitude() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "标题");
        params.put("content", "内容");
        params.put("type", 1);
        params.put("latitude", 100.0); // 超出范围
        params.put("longitude", 116.0);

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 无效经度")
    void testCreatePost_InvalidLongitude() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "标题");
        params.put("content", "内容");
        params.put("type", 1);
        params.put("latitude", 39.0);
        params.put("longitude", 200.0); // 超出范围

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 坐标(0,0)被拒绝")
    void testCreatePost_InvalidCoordinateZero() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "标题");
        params.put("content", "内容");
        params.put("type", 2);
        params.put("latitude", 0.0);
        params.put("longitude", 0.0);

        var result = postService.createPost(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    // ==================== 获取帖子列表测试 ====================

    @Test
    @DisplayName("集成测试 - 获取帖子列表（缓存未命中）")
    void testGetPostList_CacheMiss() {
        when(cacheService.get(anyString())).thenReturn(null);

        Page<Post> page = new PageImpl<>(List.of(testPost));
        when(postRepository.findByPostTypeAndStatusOrderByCreatedAtDesc(eq(1), eq(1), any(Pageable.class)))
            .thenReturn(page);

        var result = postService.getPostList(1, 1, 10);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().get("total"));
        assertEquals(1, result.getData().get("page"));
    }

    @Test
    @DisplayName("集成测试 - 获取帖子列表（缓存命中）")
    void testGetPostList_CacheHit() {
        when(cacheService.get(anyString())).thenReturn("cached_data");

        var result = postService.getPostList(1, 1, 10);

        assertTrue(result.isSuccess());
        assertTrue((Boolean) result.getData().get("fromCache"));
        verify(postRepository, never()).findByPostTypeAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("集成测试 - 获取所有类型帖子列表")
    void testGetPostList_AllTypes() {
        when(cacheService.get(anyString())).thenReturn(null);

        Page<Post> page = new PageImpl<>(List.of(testPost));
        when(postRepository.findByStatusOrderByCreatedAtDesc(eq(1), any(Pageable.class)))
            .thenReturn(page);

        var result = postService.getPostList(null, 1, 10);

        assertTrue(result.isSuccess());
        verify(postRepository, times(1)).findByStatusOrderByCreatedAtDesc(eq(1), any(Pageable.class));
    }

    @Test
    @DisplayName("集成测试 - 空列表")
    void testGetPostList_Empty() {
        when(cacheService.get(anyString())).thenReturn(null);
        when(postRepository.findByStatusOrderByCreatedAtDesc(eq(1), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        var result = postService.getPostList(1, 1, 10);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getData().get("total"));
    }

    // ==================== 点赞测试 ====================

    @Test
    @DisplayName("集成测试 - 已认证用户点赞成功")
    void testLikePost_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        var result = postService.likePost(1L, verifiedUser);

        assertTrue(result.isSuccess());
        assertEquals(1, testPost.getLikeCount());
    }

    @Test
    @DisplayName("集成测试 - 未认证用户点赞被拒绝")
    void testLikePost_UnauthUser() {
        var result = postService.likePost(1L, unauthUser);

        assertFalse(result.isSuccess());
        assertEquals(40301, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 点赞不存在的帖子")
    void testLikePost_PostNotFound() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        var result = postService.likePost(999L, verifiedUser);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 多次点赞计数")
    void testLikePost_MultipleLikes() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 点赞5次
        for (int i = 0; i < 5; i++) {
            postService.likePost(1L, verifiedUser);
        }

        assertEquals(5, testPost.getLikeCount());
    }

    // ==================== 完整业务流程测试 ====================

    @Test
    @DisplayName("完整流程 - 用户发布→查询→点赞")
    void testCompletePostFlow() {
        // 1. 发布帖子
        Map<String, Object> params = new HashMap<>();
        params.put("title", "我的第一条动态");
        params.put("content", "今天天气真好！");
        params.put("type", 1);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        var createResult = postService.createPost(verifiedUser, params);
        assertTrue(createResult.isSuccess());
        Long postId = createResult.getData().getId();

        // 2. 查询帖子列表
        when(cacheService.get(anyString())).thenReturn(null);
        Page<Post> page = new PageImpl<>(List.of(createResult.getData()));
        when(postRepository.findByStatusOrderByCreatedAtDesc(eq(1), any(Pageable.class)))
            .thenReturn(page);

        var listResult = postService.getPostList(1, 1, 10);
        assertTrue(listResult.isSuccess());
        assertEquals(1, ((List) listResult.getData().get("list")).size());

        // 3. 点赞
        when(postRepository.findById(postId)).thenReturn(Optional.of(createResult.getData()));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var likeResult = postService.likePost(postId, verifiedUser);
        assertTrue(likeResult.isSuccess());
    }

    @Test
    @DisplayName("完整流程 - 互助帖发布→查询附近任务")
    void testCompleteHelpPostFlow() {
        // 1. 发布互助帖子
        Map<String, Object> params = new HashMap<>();
        params.put("title", "帮忙取快递");
        params.put("content", "有谁能帮忙取个快递");
        params.put("type", 2);
        params.put("latitude", 39.9042);
        params.put("longitude", 116.4074);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        var createResult = postService.createPost(verifiedUser, params);
        assertTrue(createResult.isSuccess());
        assertEquals(2, createResult.getData().getPostType());

        // 2. 查询附近任务
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        var nearbyResult = postService.getNearbyOrders(39.9042, 116.4074, 5.0, 1, 10);
        assertTrue(nearbyResult.isSuccess());
    }

    @Test
    @DisplayName("权限测试 - 不同认证状态用户操作对比")
    void testPermissionComparison() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试帖子");
        params.put("content", "内容");
        params.put("type", 1);

        // 已认证用户 - 可以发布
        var verifiedResult = postService.createPost(verifiedUser, params);
        assertTrue(verifiedResult.isSuccess());

        // 未认证用户 - 不能发布
        var unauthResult = postService.createPost(unauthUser, params);
        assertFalse(unauthResult.isSuccess());
        assertEquals(40301, unauthResult.getCode());

        // 认证中用户 - 不能发布
        var pendingResult = postService.createPost(pendingUser, params);
        assertFalse(pendingResult.isSuccess());
        assertEquals(40302, pendingResult.getCode());
    }

    @Test
    @DisplayName("边界测试 - 空参数")
    void testEmptyParams() {
        Map<String, Object> emptyParams = new HashMap<>();

        var result = postService.createPost(verifiedUser, emptyParams);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("边界测试 - 特殊字符内容")
    void testSpecialCharacters() {
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试<>&\"'标题");
        params.put("content", "内容包含特殊字符：\n换行\t制表\"引号'单引");
        params.put("type", 1);

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        var result = postService.createPost(verifiedUser, params);

        assertTrue(result.isSuccess());
        assertEquals("测试<>&\"'标题", result.getData().getTitle());
        assertTrue(result.getData().getContent().contains("换行"));
    }

    @Test
    @DisplayName("性能测试 - 缓存命中性能")
    void testCacheHitPerformance() {
        when(cacheService.get(anyString())).thenReturn("cached");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            postService.getPostList(1, 1, 10);
        }
        long duration = System.currentTimeMillis() - start;

        // 缓存命中应该在100ms内完成1000次
        assertTrue(duration < 100);
    }

    private Post createTestPost(Long id, Long userId, Integer postType, String title, String content) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setPostType(postType);
        post.setTitle(title);
        post.setContent(content);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus(1);
        post.setLatitude(39.9042);
        post.setLongitude(116.4074);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return post;
    }
}
