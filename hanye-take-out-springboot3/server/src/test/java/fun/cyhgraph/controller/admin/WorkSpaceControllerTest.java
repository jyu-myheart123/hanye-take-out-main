package fun.cyhgraph.controller.admin;

import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.WorkSpaceService;
import fun.cyhgraph.vo.BusinessDataVO;
import fun.cyhgraph.vo.DishOverViewVO;
import fun.cyhgraph.vo.OrderOverViewVO;
import fun.cyhgraph.vo.SetmealOverViewVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("管理员工作台控制器单元测试")
class WorkSpaceControllerTest {

    @Mock
    private WorkSpaceService workSpaceService;

    @InjectMocks
    private WorkSpaceController workSpaceController;

    @Test
    @DisplayName("WS-ADMIN-001: 测试查询今日营业数据")
    void testBusinessData() {
        BusinessDataVO businessDataVO = new BusinessDataVO();
        businessDataVO.setTurnover(10000.0);
        businessDataVO.setValidOrderCount(100);

        when(workSpaceService.getBusinessData(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(businessDataVO);

        Result<BusinessDataVO> result = workSpaceController.businessData();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(10000.0, result.getData().getTurnover());
    }

    @Test
    @DisplayName("WS-ADMIN-002: 测试查询订单管理数据")
    void testOrderOverView() {
        OrderOverViewVO orderOverViewVO = OrderOverViewVO.builder()
                .waitingOrders(10)
                .deliveredOrders(5)
                .build();

        when(workSpaceService.getOrderOverView()).thenReturn(orderOverViewVO);

        Result<OrderOverViewVO> result = workSpaceController.orderOverView();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(10, result.getData().getWaitingOrders());
    }

    @Test
    @DisplayName("WS-ADMIN-003: 测试查询菜品总览")
    void testDishOverView() {
        DishOverViewVO dishOverViewVO = new DishOverViewVO();
        dishOverViewVO.setSold(50);
        dishOverViewVO.setDiscontinued(10);

        when(workSpaceService.getDishOverView()).thenReturn(dishOverViewVO);

        Result<DishOverViewVO> result = workSpaceController.dishOverView();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(50, result.getData().getSold());
    }

    @Test
    @DisplayName("WS-ADMIN-004: 测试查询套餐总览")
    void testSetmealOverView() {
        SetmealOverViewVO setmealOverViewVO = new SetmealOverViewVO();
        setmealOverViewVO.setSold(30);
        setmealOverViewVO.setDiscontinued(5);

        when(workSpaceService.getSetmealOverView()).thenReturn(setmealOverViewVO);

        Result<SetmealOverViewVO> result = workSpaceController.setmealOverView();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(30, result.getData().getSold());
    }
}