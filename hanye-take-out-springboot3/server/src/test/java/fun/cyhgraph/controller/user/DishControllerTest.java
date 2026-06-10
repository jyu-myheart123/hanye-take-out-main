package fun.cyhgraph.controller.user;

import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.DishService;
import fun.cyhgraph.vo.DishVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("菜品控制器单元测试")
class DishControllerTest {

    @Mock
    private DishService dishService;

    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private DishController dishController;

    @Test
    @DisplayName("DISH-001: 测试从Redis获取菜品列表")
    void testGetDishList_FromRedis() {
        List<DishVO> dishList = new ArrayList<>();
        DishVO dish = new DishVO();
        dish.setId(1);
        dish.setName("宫保鸡丁");
        dishList.add(dish);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("dish_1")).thenReturn(dishList);
        
        Result<List<DishVO>> result = dishController.getDishList(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("宫保鸡丁", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("DISH-002: 测试获取菜品详情")
    void testGetDishDetail() {
        DishVO dishVO = new DishVO();
        dishVO.setId(1);
        dishVO.setName("麻婆豆腐");
        
        when(dishService.getDishWithFlavorById(1)).thenReturn(dishVO);
        
        Result<DishVO> result = dishController.getDish(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("麻婆豆腐", result.getData().getName());
    }
}