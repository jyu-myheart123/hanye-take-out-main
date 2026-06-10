package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.DishDTO;
import fun.cyhgraph.dto.DishPageDTO;
import fun.cyhgraph.result.PageResult;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理员菜品控制器单元测试")
class DishControllerTest {

    @Mock
    private DishService dishService;

    @Mock
    private RedisTemplate redisTemplate;

    @InjectMocks
    private DishController dishController;

    @Test
    @DisplayName("DISH-ADMIN-001: 测试新增菜品")
    void testAddDish() {
        DishDTO dishDTO = new DishDTO();
        dishDTO.setName("测试菜品");
        dishDTO.setPrice(new BigDecimal("10.0"));
        dishDTO.setCategoryId(1);

        when(redisTemplate.keys(any())).thenReturn(new HashSet<>());

        Result result = dishController.addDishWithFlavor(dishDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(dishService).addDishWithFlavor(any(DishDTO.class));
    }

    @Test
    @DisplayName("DISH-ADMIN-002: 测试菜品分页查询")
    void testDishPage() {
        DishPageDTO pageDTO = new DishPageDTO();
        pageDTO.setPage(1);
        pageDTO.setPageSize(10);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(50L);

        when(dishService.getPageList(any(DishPageDTO.class))).thenReturn(pageResult);

        Result<PageResult> result = dishController.getPageList(pageDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(50L, result.getData().getTotal());
    }

    @Test
    @DisplayName("DISH-ADMIN-003: 测试修改菜品")
    void testUpdateDish() {
        DishDTO dishDTO = new DishDTO();
        dishDTO.setId(1);
        dishDTO.setName("新菜品名");

        when(redisTemplate.keys(any())).thenReturn(new HashSet<>());

        Result result = dishController.updateDishWithFlavor(dishDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(dishService).updateDishWithFlavor(any(DishDTO.class));
    }

    @Test
    @DisplayName("DISH-ADMIN-004: 测试批量删除菜品")
    void testDeleteBatchDish() {
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2);

        when(redisTemplate.keys(any())).thenReturn(new HashSet<>());

        Result result = dishController.deleteBatch(ids);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(dishService).deleteBatch(any(List.class));
    }

    @Test
    @DisplayName("DISH-ADMIN-005: 测试启用禁用菜品")
    void testOnOffDish() {
        when(redisTemplate.keys(any())).thenReturn(new HashSet<>());

        Result result = dishController.onOff(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(dishService).onOff(1);
    }

    @Test
    @DisplayName("DISH-ADMIN-006: 测试查询菜品详情")
    void testGetDishDetail() {
        DishVO dishVO = new DishVO();
        dishVO.setId(1);
        dishVO.setName("测试菜品");

        when(dishService.getDishWithFlavorById(1)).thenReturn(dishVO);

        Result<DishVO> result = dishController.getDishWithFlavorById(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("测试菜品", result.getData().getName());
    }
}