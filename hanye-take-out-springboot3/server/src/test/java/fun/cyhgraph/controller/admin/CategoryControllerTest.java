package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.CategoryDTO;
import fun.cyhgraph.dto.CategoryTypePageDTO;
import fun.cyhgraph.entity.Category;
import fun.cyhgraph.result.PageResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理员分类控制器单元测试")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    @DisplayName("CAT-ADMIN-001: 测试新增分类")
    void testAddCategory() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setName("测试分类");
        categoryDTO.setType(1);

        Result result = categoryController.addCategory(categoryDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(categoryService).addCategory(any(CategoryDTO.class));
    }

    @Test
    @DisplayName("CAT-ADMIN-002: 测试分页查询分类")
    void testCategoryPage() {
        CategoryTypePageDTO pageDTO = new CategoryTypePageDTO();
        pageDTO.setPage(1);
        pageDTO.setPageSize(10);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(20L);

        when(categoryService.getPageList(any(CategoryTypePageDTO.class))).thenReturn(pageResult);

        Result<PageResult> result = categoryController.getPageList(pageDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(20L, result.getData().getTotal());
    }

    @Test
    @DisplayName("CAT-ADMIN-003: 测试修改分类")
    void testUpdateCategory() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(1);
        categoryDTO.setName("新名称");

        Result result = categoryController.udpate(categoryDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(categoryService).udpate(any(CategoryDTO.class));
    }

    @Test
    @DisplayName("CAT-ADMIN-004: 测试删除分类")
    void testDeleteCategory() {
        Result result = categoryController.delete(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(categoryService).delete(1);
    }

    @Test
    @DisplayName("CAT-ADMIN-005: 测试启用禁用分类")
    void testOnOffCategory() {
        Result result = categoryController.onOff(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(categoryService).onOff(1);
    }

    @Test
    @DisplayName("CAT-ADMIN-006: 测试根据ID查询分类")
    void testGetCategoryById() {
        Category category = new Category();
        category.setId(1);
        category.setName("测试分类");

        when(categoryService.getById(1)).thenReturn(category);

        Result<Category> result = categoryController.getById(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("测试分类", result.getData().getName());
    }
}