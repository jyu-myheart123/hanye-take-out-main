package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.SetmealDTO;
import fun.cyhgraph.dto.SetmealPageDTO;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.SetmealService;
import fun.cyhgraph.vo.SetmealVO;
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
@DisplayName("管理员套餐控制器单元测试")
class SetmealControllerTest {

    @Mock
    private SetmealService setmealService;

    @InjectMocks
    private SetmealController setmealController;

    @Test
    @DisplayName("SET-ADMIN-001: 测试新增套餐")
    void testAddSetmeal() {
        SetmealDTO setmealDTO = new SetmealDTO();
        setmealDTO.setName("测试套餐");

        Result result = setmealController.addSetmeal(setmealDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(setmealService).addSetmeal(any(SetmealDTO.class));
    }

    @Test
    @DisplayName("SET-ADMIN-002: 测试套餐分页查询")
    void testSetmealPage() {
        SetmealPageDTO pageDTO = new SetmealPageDTO();
        pageDTO.setPage(1);
        pageDTO.setPageSize(10);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(30L);

        when(setmealService.getPageList(any(SetmealPageDTO.class))).thenReturn(pageResult);

        Result<PageResult> result = setmealController.getPageList(pageDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(30L, result.getData().getTotal());
    }

    @Test
    @DisplayName("SET-ADMIN-003: 测试修改套餐")
    void testUpdateSetmeal() {
        SetmealDTO setmealDTO = new SetmealDTO();
        setmealDTO.setId(1);
        setmealDTO.setName("新套餐名");

        Result result = setmealController.update(setmealDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(setmealService).update(any(SetmealDTO.class));
    }

    @Test
    @DisplayName("SET-ADMIN-004: 测试批量删除套餐")
    void testDeleteBatchSetmeal() {
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2);

        Result result = setmealController.deleteBatch(ids);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(setmealService).deleteBatch(any(List.class));
    }

    @Test
    @DisplayName("SET-ADMIN-005: 测试启用禁用套餐")
    void testOnOffSetmeal() {
        Result result = setmealController.onOff(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(setmealService).onOff(1);
    }

    @Test
    @DisplayName("SET-ADMIN-006: 测试查询套餐详情")
    void testGetSetmealDetail() {
        SetmealVO setmealVO = new SetmealVO();
        setmealVO.setId(1);
        setmealVO.setName("测试套餐");

        when(setmealService.getSetmealById(1)).thenReturn(setmealVO);

        Result<SetmealVO> result = setmealController.getSetmealById(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("测试套餐", result.getData().getName());
    }
}