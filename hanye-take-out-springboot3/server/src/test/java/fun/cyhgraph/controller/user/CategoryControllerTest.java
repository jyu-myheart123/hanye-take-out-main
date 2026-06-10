package fun.cyhgraph.controller.user;

import fun.cyhgraph.entity.Category;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.CategoryService;
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
@DisplayName("分类控制器单元测试")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    @DisplayName("CAT-001: 测试获取菜品分类列表")
    void testGetDishCategoryList() {
        List<Category> categoryList = new ArrayList<>();
        Category category = new Category();
        category.setId(1);
        category.setName("热销菜品");
        category.setType(1);
        categoryList.add(category);
        
        when(categoryService.getList(1)).thenReturn(categoryList);
        
        Result<List<Category>> result = categoryController.list(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("热销菜品", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("CAT-002: 测试获取套餐分类列表")
    void testGetSetmealCategoryList() {
        List<Category> categoryList = new ArrayList<>();
        Category category = new Category();
        category.setId(2);
        category.setName("超值套餐");
        category.setType(2);
        categoryList.add(category);
        
        when(categoryService.getList(2)).thenReturn(categoryList);
        
        Result<List<Category>> result = categoryController.list(2);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("超值套餐", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("CAT-003: 测试获取所有分类")
    void testGetAllCategoryList() {
        List<Category> categoryList = new ArrayList<>();
        when(categoryService.getList(null)).thenReturn(categoryList);
        
        Result<List<Category>> result = categoryController.list(null);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }
}