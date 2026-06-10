package fun.cyhgraph.controller.user;

import fun.cyhgraph.entity.Setmeal;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.SetmealService;
import fun.cyhgraph.vo.SetmealWithPicVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("套餐控制器单元测试")
class SetmealControllerTest {

    @Mock
    private SetmealService setmealService;

    @InjectMocks
    private SetmealController setmealController;

    @Test
    @DisplayName("SET-001: 测试获取套餐列表")
    void testGetSetmealList() {
        List<Setmeal> setmealList = new ArrayList<>();
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1);
        setmeal.setName("超值套餐A");
        setmealList.add(setmeal);
        
        when(setmealService.getList(1)).thenReturn(setmealList);
        
        Result<List<Setmeal>> result = setmealController.getSetmealList(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("超值套餐A", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("SET-002: 测试获取套餐详情")
    void testGetSetmealDetail() {
        SetmealWithPicVO setmealVO = new SetmealWithPicVO();
        setmealVO.setId(1);
        setmealVO.setName("家庭套餐");
        
        when(setmealService.getSetmealWithPic(1)).thenReturn(setmealVO);
        
        Result<SetmealWithPicVO> result = setmealController.getSetmeal(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("家庭套餐", result.getData().getName());
    }

    @Test
    @DisplayName("SET-003: 测试获取套餐包含的菜品")
    void testGetSetmealDishes() {
        List<fun.cyhgraph.vo.DishItemVO> dishList = new ArrayList<>();
        fun.cyhgraph.vo.DishItemVO dish = new fun.cyhgraph.vo.DishItemVO();
        dish.setName("菜品1");
        dishList.add(dish);
        
        when(setmealService.getSetmealDishesById(1)).thenReturn(dishList);
        
        Result<List<fun.cyhgraph.vo.DishItemVO>> result = setmealController.getSetmealDishes(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }
}