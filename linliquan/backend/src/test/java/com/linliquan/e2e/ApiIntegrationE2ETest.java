package com.linliquan.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.service.AuthService;
import com.linliquan.service.CacheService;
import com.linliquan.service.CommentService;
import com.linliquan.service.OrderService;
import com.linliquan.service.PostService;
import com.linliquan.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("端到端接口集成测试")
class ApiIntegrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PostService postService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CacheService cacheService;

    private String verifiedToken;
    private String pendingToken;
    private String unauthToken;

    private User verifiedUser;
    private User pendingUser;
    private User unauthUser;

    @BeforeEach
    void setUp() {
        verifiedUser = createTestUser(1L, VerificationStatus.VERIFIED);
        pendingUser = createTestUser(2L, VerificationStatus.PENDING);
        unauthUser = createTestUser(3L, VerificationStatus.UNAUTH);

        verifiedToken = JwtUtil.generateToken(1L, "hash_verified");
        pendingToken = JwtUtil.generateToken(2L, "hash_pending");
        unauthToken = JwtUtil.generateToken(3L, "hash_unauth");
    }

    private User createTestUser(Long id, VerificationStatus status) {
        User user = new User();
        user.setId(id);
        user.setPhoneHash("hash_" + status.name().toLowerCase());
        user.setNickname("测试用户-" + status.getDescription());
        user.setVerificationStatus(status);
        return user;
    }

    @Nested
    @DisplayName("认证模块接口测试")
    class AuthApiTests {

        @Test
        @DisplayName("POST /v1/auth/login - 登录成功")
        void testLogin_Success() throws Exception {
            Map<String, Object> loginResult = new HashMap<>();
            loginResult.put("user", verifiedUser);
            loginResult.put("accessToken", verifiedToken);

            when(authService.login("13800138000"))
                .thenReturn(com.linliquan.common.Result.success(loginResult));

            mockMvc.perform(post("/v1/auth/login")
                    .param("phone", "13800138000")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.id").value(1));
        }

        @Test
        @DisplayName("POST /v1/auth/login-mock - Mock登录三种状态")
        void testLoginMock_AllStatuses() throws Exception {
            mockMvc.perform(post("/v1/auth/login-mock")
                    .param("statusType", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.verificationStatus").value(0));

            mockMvc.perform(post("/v1/auth/login-mock")
                    .param("statusType", "1"))
                .andExpect(jsonPath("$.data.user.verificationStatus").value(1));

            mockMvc.perform(post("/v1/auth/login-mock")
                    .param("statusType", "2"))
                .andExpect(jsonPath("$.data.user.verificationStatus").value(2));
        }

        @Test
        @DisplayName("GET /v1/auth/status - 未登录状态查询")
        void testGetStatus_NotLoggedIn() throws Exception {
            mockMvc.perform(get("/v1/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value(0))
                .andExpect(jsonPath("$.data.canWrite").value(false))
                .andExpect(jsonPath("$.data.canRead").value(true));
        }

        @Test
        @DisplayName("POST /v1/auth/apply-verification - 提交认证申请")
        void testApplyVerification_Success() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("idCard", "110101199001011234");
            requestBody.put("houseNumber", "1-101");
            requestBody.put("certificateUrl", "https://cos.com/cert.jpg");

            when(authService.applyVerification(eq(1L), anyString(), anyString(), anyString()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/auth/apply-verification")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("帖子模块接口测试")
    class PostApiTests {

        @Test
        @DisplayName("GET /v1/posts - 获取帖子列表")
        void testGetPostList_Success() throws Exception {
            when(postService.getPostList(anyInt(), anyInt(), anyInt()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/posts")
                    .param("type", "1")
                    .param("page", "1")
                    .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /v1/posts - 已认证用户发布帖子")
        void testCreatePost_VerifiedUser() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", "测试帖子标题");
            requestBody.put("content", "这是测试帖子内容");
            requestBody.put("postType", 1);
            requestBody.put("lat", 39.9042);
            requestBody.put("lng", 116.4074);

            when(postService.createPost(any(), anyMap()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/posts - 未认证用户发布被拦截(403)")
        void testCreatePost_UnauthUser_Forbidden() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", "测试帖子");
            requestBody.put("content", "内容");
            requestBody.put("postType", 1);

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /v1/posts - 认证中用户发布被拦截(403)")
        void testCreatePost_PendingUser_Forbidden() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", "测试帖子");
            requestBody.put("content", "内容");
            requestBody.put("postType", 1);

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + pendingToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /v1/posts/nearby - 获取附近帖子")
        void testGetNearbyPosts_Success() throws Exception {
            when(postService.getNearbyOrders(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/posts/nearby")
                    .param("lat", "39.9042")
                    .param("lng", "116.4074")
                    .param("radius", "5")
                    .param("page", "1")
                    .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /v1/posts/{id}/like - 点赞帖子")
        void testToggleLike_Success() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("liked", true);

            when(postService.toggleLike(eq(1L), any(), eq(true)))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/posts/1/like")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/posts/{id}/like - 未认证用户点赞被拦截")
        void testToggleLike_Unauth_Forbidden() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("liked", true);

            mockMvc.perform(post("/v1/posts/1/like")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("评论模块接口测试")
    class CommentApiTests {

        @Test
        @DisplayName("GET /v1/comments - 获取评论列表")
        void testGetCommentList_Success() throws Exception {
            when(commentService.getCommentList(eq(1L), anyInt(), anyInt()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/comments")
                    .param("postId", "1")
                    .param("page", "1")
                    .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /v1/comments - 已认证用户发表评论")
        void testCreateComment_VerifiedUser() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("postId", 1);
            requestBody.put("content", "这是一条测试评论");

            when(commentService.createComment(any(), eq(1L), eq("这是一条测试评论"), isNull(), isNull()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/comments")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/comments - 未认证用户发表评论被拦截")
        void testCreateComment_Unauth_Forbidden() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("postId", 1);
            requestBody.put("content", "评论内容");

            mockMvc.perform(post("/v1/comments")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /v1/comments/{id} - 删除评论")
        void testDeleteComment_Success() throws Exception {
            when(commentService.deleteComment(any(), eq(1L)))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(delete("/v1/comments/1")
                    .header("Authorization", "Bearer " + verifiedToken))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/comments/{id}/like - 点赞评论")
        void testLikeComment_Success() throws Exception {
            when(commentService.likeComment(eq(1L), any()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/comments/1/like")
                    .header("Authorization", "Bearer " + verifiedToken))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("互助任务模块接口测试")
    class OrderApiTests {

        @Test
        @DisplayName("GET /v1/orders/nearby - 获取附近互助任务")
        void testGetNearbyOrders_Success() throws Exception {
            when(orderService.getNearbyOrders(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/orders/nearby")
                    .param("lat", "39.9042")
                    .param("lng", "116.4074")
                    .param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /v1/orders - 已认证用户发布互助任务")
        void testCreateOrder_VerifiedUser() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("postId", 1);
            requestBody.put("helpType", 1);
            requestBody.put("rewardAmount", 10.00);
            requestBody.put("lat", 39.9042);
            requestBody.put("lng", 116.4074);

            when(orderService.createOrder(any(), anyMap()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/orders")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/orders - 未认证用户发布被拦截")
        void testCreateOrder_Unauth_Forbidden() throws Exception {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("postId", 1);
            requestBody.put("helpType", 1);

            mockMvc.perform(post("/v1/orders")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /v1/orders/{id}/accept - 已认证用户接单")
        void testAcceptOrder_VerifiedUser() throws Exception {
            when(orderService.acceptOrder(eq(1L), any()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/orders/1/accept")
                    .header("Authorization", "Bearer " + verifiedToken))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/orders/{id}/accept - 认证中用户接单被拦截")
        void testAcceptOrder_Pending_Forbidden() throws Exception {
            mockMvc.perform(post("/v1/orders/1/accept")
                    .header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("权限红线规则测试")
    class PermissionRedlineTests {

        @Test
        @DisplayName("写操作 - 未带Token返回401")
        void testWriteOperation_NoToken_Unauthorized() throws Exception {
            mockMvc.perform(post("/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("写操作 - 无效Token返回401")
        void testWriteOperation_InvalidToken_Unauthorized() throws Exception {
            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer invalid_token_12345")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("读操作 - 未登录可访问帖子列表")
        void testReadOperation_NoToken_Allowed() throws Exception {
            when(postService.getPostList(anyInt(), anyInt(), anyInt()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("读操作 - 未登录可查看评论")
        void testReadComments_NoToken_Allowed() throws Exception {
            when(commentService.getCommentList(eq(1L), anyInt(), anyInt()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/comments")
                    .param("postId", "1"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("读操作 - 未登录可查看附近任务")
        void testReadNearbyOrders_NoToken_Allowed() throws Exception {
            when(orderService.getNearbyOrders(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/orders/nearby")
                    .param("lat", "39.9042")
                    .param("lng", "116.4074"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST写操作 - 三态权限矩阵验证")
        void testPostWrite_PermissionMatrix() throws Exception {
            String body = "{\"title\":\"t\",\"content\":\"c\",\"postType\":1}";

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isForbidden());

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + pendingToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isForbidden());

            when(postService.createPost(any(), anyMap()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE写操作 - 三态权限矩阵验证")
        void testDeleteWrite_PermissionMatrix() throws Exception {
            mockMvc.perform(delete("/v1/comments/1")
                    .header("Authorization", "Bearer " + unauthToken))
                .andExpect(status().isForbidden());

            mockMvc.perform(delete("/v1/comments/1")
                    .header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isForbidden());

            when(commentService.deleteComment(any(), eq(1L)))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(delete("/v1/comments/1")
                    .header("Authorization", "Bearer " + verifiedToken))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("完整业务流程测试")
    class FullWorkflowTests {

        @Test
        @DisplayName("完整流程 - 登录→浏览→发布→点赞→评论")
        void testFullWorkflow_PostInteraction() throws Exception {
            Map<String, Object> loginResult = new HashMap<>();
            loginResult.put("user", verifiedUser);
            loginResult.put("accessToken", verifiedToken);

            when(authService.login("13800138000"))
                .thenReturn(com.linliquan.common.Result.success(loginResult));

            String loginResponse = mockMvc.perform(post("/v1/auth/login")
                    .param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

            when(postService.getPostList(anyInt(), anyInt(), anyInt()))
                .thenReturn(com.linliquan.common.Result.success(new HashMap<>()));

            mockMvc.perform(get("/v1/posts")
                    .header("Authorization", "Bearer " + verifiedToken))
                .andExpect(status().isOk());

            when(postService.createPost(any(), anyMap()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"测试\",\"content\":\"内容\",\"postType\":1}"))
                .andExpect(status().isOk());

            when(postService.toggleLike(eq(1L), any(), eq(true)))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/posts/1/like")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"liked\":true}"))
                .andExpect(status().isOk());

            when(commentService.createComment(any(), eq(1L), eq("好贴"), isNull(), isNull()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/comments")
                    .header("Authorization", "Bearer " + verifiedToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"postId\":1,\"content\":\"好贴\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("完整流程 - 新用户:未认证→申请认证→等待审核")
        void testFullWorkflow_NewUserVerification() throws Exception {
            Map<String, Object> loginResult = new HashMap<>();
            loginResult.put("user", unauthUser);
            loginResult.put("accessToken", unauthToken);

            when(authService.login("13900139000"))
                .thenReturn(com.linliquan.common.Result.success(loginResult));

            mockMvc.perform(post("/v1/auth/login")
                    .param("phone", "13900139000"))
                .andExpect(jsonPath("$.data.user.verificationStatus").value(0));

            mockMvc.perform(post("/v1/posts")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"t\",\"content\":\"c\",\"postType\":1}"))
                .andExpect(status().isForbidden());

            when(authService.applyVerification(eq(3L), anyString(), anyString(), anyString()))
                .thenReturn(com.linliquan.common.Result.success(null));

            mockMvc.perform(post("/v1/auth/apply-verification")
                    .header("Authorization", "Bearer " + unauthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idCard\":\"110101199001011234\",\"houseNumber\":\"1-101\"}"))
                .andExpect(status().isOk());
        }
    }
}
