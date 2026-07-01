package com.linliquan.repository;

import com.linliquan.model.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PostRepository单元测试
 * 测试Post实体的数据访问层方法
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostRepository单元测试")
class PostRepositoryTest {

    @Mock
    private JpaRepository<Post, Long> jpaRepository;

    @InjectMocks
    private PostRepository postRepository;

    private Post testPost;
    private Post testPost2;

    @BeforeEach
    void setUp() {
        testPost = createTestPost(1L, 1L, 1, "测试帖子1", "内容1", 1);
        testPost2 = createTestPost(2L, 2L, 2, "测试帖子2", "内容2", 1);
    }

    @Test
    @DisplayName("测试findById - 成功查找帖子")
    void testFindById_Success() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(testPost));

        Optional<Post> result = postRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("测试帖子1", result.get().getTitle());
        verify(jpaRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("测试findById - 帖子不存在")
    void testFindById_NotFound() {
        when(jpaRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Post> result = postRepository.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("测试findByPostTypeAndStatusOrderByCreatedAtDesc - 按类型和状态分页查询")
    void testFindByPostTypeAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost, testPost2));
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(page);

        Page<Post> result = postRepository.findByPostTypeAndStatusOrderByCreatedAtDesc(1, 1, pageable);

        assertNotNull(result);
        verify(jpaRepository, times(1)).findAll(any(), eq(pageable));
    }

    @Test
    @DisplayName("测试findByStatusOrderByCreatedAtDesc - 按状态分页查询")
    void testFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost, testPost2));
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(page);

        Page<Post> result = postRepository.findByStatusOrderByCreatedAtDesc(1, pageable);

        assertNotNull(result);
        verify(jpaRepository, times(1)).findAll(any(), eq(pageable));
    }

    @Test
    @DisplayName("测试findByUserIdAndStatusOrderByCreatedAtDesc - 查询用户帖子")
    void testFindByUserIdAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost));
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(page);

        Page<Post> result = postRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, 1, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("测试countByUserIdAndStatus - 统计用户帖子数")
    void testCountByUserIdAndStatus() {
        when(jpaRepository.count(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(5L);

        long count = postRepository.countByUserIdAndStatus(1L, 1);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("测试save - 保存新帖子")
    void testSave_NewPost() {
        when(jpaRepository.save(any(Post.class))).thenReturn(testPost);

        Post savedPost = postRepository.save(testPost);

        assertNotNull(savedPost);
        assertEquals(1L, savedPost.getId());
        verify(jpaRepository, times(1)).save(testPost);
    }

    @Test
    @DisplayName("测试save - 更新帖子")
    void testSave_UpdatePost() {
        testPost.setTitle("更新后的标题");
        when(jpaRepository.save(any(Post.class))).thenReturn(testPost);

        Post updated = postRepository.save(testPost);

        assertEquals("更新后的标题", updated.getTitle());
    }

    @Test
    @DisplayName("测试deleteById - 删除帖子")
    void testDeleteById() {
        doNothing().when(jpaRepository).deleteById(anyLong());

        postRepository.deleteById(1L);

        verify(jpaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("测试findAll - 查询所有帖子")
    void testFindAll() {
        when(jpaRepository.findAll()).thenReturn(Arrays.asList(testPost, testPost2));

        List<Post> posts = postRepository.findAll();

        assertEquals(2, posts.size());
    }

    @Test
    @DisplayName("边界测试 - 空结果集")
    void testFindAll_EmptyResult() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<Post> posts = postRepository.findAll();

        assertTrue(posts.isEmpty());
    }

    @Test
    @DisplayName("边界测试 - 分页参数验证")
    void testPaginationParameters() {
        Pageable validPageable = PageRequest.of(0, 10);
        Pageable emptyPageable = PageRequest.of(0, 0);
        Pageable negativePage = PageRequest.of(-1, 10);

        assertThrows(Exception.class, () -> postRepository.findAll(emptyPageable));
        assertThrows(Exception.class, () -> postRepository.findAll(negativePage));
    }

    @Test
    @DisplayName("场景测试 - 帖子发布与删除流程")
    void testPostLifecycle() {
        // 1. 创建帖子
        when(jpaRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });

        Post newPost = createTestPost(null, 1L, 1, "新帖子", "新内容", 1);
        Post saved = postRepository.save(newPost);
        assertNotNull(saved.getId());

        // 2. 查询帖子
        when(jpaRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
        Optional<Post> found = postRepository.findById(saved.getId());
        assertTrue(found.isPresent());

        // 3. 更新帖子
        found.get().setStatus(2); // 标记为已删除
        when(jpaRepository.save(any(Post.class))).thenReturn(found.get());
        Post updated = postRepository.save(found.get());
        assertEquals(2, updated.getStatus());

        // 4. 删除帖子
        doNothing().when(jpaRepository).deleteById(anyLong());
        postRepository.deleteById(saved.getId());
        verify(jpaRepository, times(1)).deleteById(saved.getId());
    }

    @Test
    @DisplayName("场景测试 - 不同类型帖子查询")
    void testDifferentPostTypes() {
        Post infoPost = createTestPost(1L, 1L, 1, "信息帖", "信息内容", 1);
        Post helpPost = createTestPost(2L, 2L, 2, "互助帖", "互助内容", 1);

        when(jpaRepository.findAll()).thenReturn(Arrays.asList(infoPost, helpPost));

        List<Post> allPosts = postRepository.findAll();
        assertEquals(2, allPosts.size());

        // 过滤信息广场帖子
        List<Post> infoPosts = allPosts.stream()
            .filter(p -> p.getPostType() == 1)
            .toList();
        assertEquals(1, infoPosts.size());
        assertEquals("信息帖", infoPosts.get(0).getTitle());

        // 过滤互助帖子
        List<Post> helpPosts = allPosts.stream()
            .filter(p -> p.getPostType() == 2)
            .toList();
        assertEquals(1, helpPosts.size());
        assertEquals("互助帖", helpPosts.get(0).getTitle());
    }

    private Post createTestPost(Long id, Long userId, Integer postType, String title, String content, Integer status) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setPostType(postType);
        post.setTitle(title);
        post.setContent(content);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus(status);
        post.setLatitude(39.9042);
        post.setLongitude(116.4074);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return post;
    }
}
