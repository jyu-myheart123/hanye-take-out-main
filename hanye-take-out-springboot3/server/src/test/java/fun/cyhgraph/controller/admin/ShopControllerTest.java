package fun.cyhgraph.controller.admin;

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
@DisplayName("管理员店铺控制器单元测试")
class ShopControllerTest {

    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOperations;

    @InjectMocks
    private ShopController shopController;

    @Test
    @DisplayName("SHOP-ADMIN-001: 测试设置店铺状态为营业中")
    void testSetStatus_Open() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Result result = shopController.setStatus(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(valueOperations).set("SHOP_STATUS", 1);
    }

    @Test
    @DisplayName("SHOP-ADMIN-002: 测试设置店铺状态为打烊")
    void testSetStatus_Closed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Result result = shopController.setStatus(0);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(valueOperations).set("SHOP_STATUS", 0);
    }

    @Test
    @DisplayName("SHOP-ADMIN-003: 测试获取店铺营业状态")
    void testGetStatus() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("SHOP_STATUS")).thenReturn(1);

        Result<Integer> result = shopController.getStatus();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
    }

    @Test
    @DisplayName("SHOP-ADMIN-004: 测试获取店铺打烊状态")
    void testGetStatus_Closed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("SHOP_STATUS")).thenReturn(0);

        Result<Integer> result = shopController.getStatus();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData());
    }
}
