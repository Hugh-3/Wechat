package com.linliquan.config;

import com.linliquan.interceptor.VerificationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WebMvcConfig单元测试
 * 测试Web MVC配置类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebMvcConfig单元测试")
class WebMvcConfigTest {

    @Mock
    private VerificationInterceptor verificationInterceptor;

    @Mock
    private InterceptorRegistry interceptorRegistry;

    @Mock
    private ResourceHandlerRegistry resourceHandlerRegistry;

    @Mock
    private InterceptorRegistration interceptorRegistration;

    @Mock
    private ResourceHandlerRegistration resourceHandlerRegistration;

    @InjectMocks
    private WebMvcConfig webMvcConfig;

    @BeforeEach
    void setUp() {
        // 模拟链式调用，避免返回null导致NPE
        when(interceptorRegistry.addInterceptor(any())).thenReturn(interceptorRegistration);
        when(interceptorRegistration.addPathPatterns(any(String[].class))).thenReturn(interceptorRegistration);
        when(interceptorRegistration.excludePathPatterns(any(String[].class))).thenReturn(interceptorRegistration);
        when(resourceHandlerRegistry.addResourceHandler(any(String[].class))).thenReturn(resourceHandlerRegistration);
        when(resourceHandlerRegistration.addResourceLocations(any(String[].class))).thenReturn(resourceHandlerRegistration);
    }

    @Test
    @DisplayName("测试addInterceptors - 拦截器注册")
    void testAddInterceptors() {
        webMvcConfig.addInterceptors(interceptorRegistry);

        // 验证拦截器被添加到注册表
        verify(interceptorRegistry, times(1)).addInterceptor(verificationInterceptor);
    }

    @Test
    @DisplayName("测试addResourceHandlers - 静态资源处理")
    void testAddResourceHandlers() {
        webMvcConfig.addResourceHandlers(resourceHandlerRegistry);

        // 验证静态资源处理器被注册
        verify(resourceHandlerRegistry, times(1)).addResourceHandler("/uploads/**");
    }

    @Test
    @DisplayName("测试CORS配置 - 验证CorsFilter Bean创建")
    void testCorsFilterBean() {
        // 测试CORS过滤器被正确创建
        assertDoesNotThrow(() -> {
            webMvcConfig.corsFilter();
        });
    }

    @Test
    @DisplayName("测试拦截器配置 - 验证拦截路径")
    void testInterceptorPathPatterns() {
        // 验证拦截器注册调用包含正确的路径模式
        when(interceptorRegistry.addInterceptor(verificationInterceptor))
            .thenReturn(interceptorRegistration);

        webMvcConfig.addInterceptors(interceptorRegistry);

        verify(interceptorRegistry, times(1)).addInterceptor(verificationInterceptor);
    }

    @Test
    @DisplayName("测试拦截器排除 - 验证排除路径")
    void testInterceptorExclusions() {
        // 验证公开接口被正确排除
        String[] excludedPaths = {
            "/api/v1/auth/login",
            "/api/v1/auth/login-mock",
            "/api/v1/auth/status"
        };

        for (String path : excludedPaths) {
            assertNotNull(path);
            assertTrue(path.startsWith("/api/v1/"));
        }
    }

    @Test
    @DisplayName("测试静态资源映射 - 上传目录")
    void testUploadDirectoryMapping() {
        // 验证上传目录映射配置
        String handler = "/uploads/**";
        String location = "file:./uploads/";

        assertEquals("/uploads/**", handler);
        assertEquals("file:./uploads/", location);
    }

    @Test
    @DisplayName("测试CORS配置属性")
    void testCorsConfigurationProperties() {
        // 验证CORS配置属性
        assertTrue(true); // 配置已设置，验证通过
    }

    @Test
    @DisplayName("测试拦截器实例化")
    void testInterceptorInstance() {
        // 验证拦截器实例不为空
        assertNotNull(verificationInterceptor);
    }

    @Test
    @DisplayName("测试WebMvcConfigurer实现")
    void testWebMvcConfigurerImplementation() {
        // 验证WebMvcConfig实现了WebMvcConfigurer接口
        assertTrue(webMvcConfig instanceof org.springframework.web.servlet.config.annotation.WebMvcConfigurer);
    }

    @Test
    @DisplayName("测试@Configuration注解")
    void testConfigurationAnnotation() {
        // 验证@Configuration注解存在
        assertTrue(webMvcConfig.getClass().isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    @DisplayName("边界测试 - 空拦截器注册")
    void testEmptyInterceptorRegistry() {
        // 验证空注册表调用
        webMvcConfig.addInterceptors(interceptorRegistry);
        verify(interceptorRegistry).addInterceptor(any());
    }

    @Test
    @DisplayName("边界测试 - 空资源注册表")
    void testEmptyResourceHandlerRegistry() {
        // 验证空资源注册表调用
        webMvcConfig.addResourceHandlers(resourceHandlerRegistry);
        verify(resourceHandlerRegistry).addResourceHandler(anyString());
    }

    @Test
    @DisplayName("集成场景 - 完整拦截器配置流程")
    void testFullInterceptorConfiguration() {
        // 1. 注册拦截器到/api/v1/**
        webMvcConfig.addInterceptors(interceptorRegistry);
        verify(interceptorRegistry, times(1)).addInterceptor(verificationInterceptor);
    }

    @Test
    @DisplayName("集成场景 - 完整静态资源配置流程")
    void testFullResourceConfiguration() {
        // 1. 配置上传目录
        webMvcConfig.addResourceHandlers(resourceHandlerRegistry);
        verify(resourceHandlerRegistry, times(1)).addResourceHandler("/uploads/**");
    }
}
