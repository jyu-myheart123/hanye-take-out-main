package fun.cyhgraph.controller.user;

import fun.cyhgraph.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("店铺控制器单元测试")
class ShopControllerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ShopController shopController;

    @Test
    @DisplayName("SC-001: 测试获取店铺状态-Redis正常，返回营业中")
    void testGetStatus_RedisNormal_Open() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("SHOP_STATUS")).thenReturn(1);
        
        Result<Integer> result = shopController.getStatus();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
    }

    @Test
    @DisplayName("SC-002: 测试获取店铺状态-Redis正常，返回打烊")
    void testGetStatus_RedisNormal_losed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("SHOP_STATUS")).thenReturn(0);
        
        Result<Integer> result = shopController.getStatus();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData());
    }

    @Test
    @DisplayName("SC-003: 测试获取店铺状态-Redis返回null，默认营业中")
    void testGetStatus_RedisNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("SHOP_STATUS")).thenReturn(null);
        
        Result<Integer> result = shopController.getStatus();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
    }

    @Test
    @DisplayName("SC-004: 测试获取店铺状态-Redis连接异常，默认营业中")
    void testGetStatus_RedisConnectionError() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis连接失败"));
        
        Result<Integer> result = shopController.getStatus();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
    }

    @Test
    @DisplayName("SC-005: 测试获取店铺状态-Redis超时异常，默认营业中")
    void testGetStatus_RedisTimeout() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis timeout"));
        
        Result<Integer> result = shopController.getStatus();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
    }
}