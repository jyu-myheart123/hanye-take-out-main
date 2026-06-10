package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.OrderCancelDTO;
import fun.cyhgraph.dto.OrderConfirmDTO;
import fun.cyhgraph.dto.OrderPageDTO;
import fun.cyhgraph.dto.OrderRejectionDTO;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.OrderService;
import fun.cyhgraph.vo.OrderStatisticsVO;
import fun.cyhgraph.vo.OrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("管理员订单控制器单元测试")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    @DisplayName("ORD-ADMIN-001: 测试条件分页查询订单")
    void testConditionSearch() {
        OrderPageDTO pageDTO = new OrderPageDTO();
        pageDTO.setPage(1);
        pageDTO.setPageSize(10);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(50L);

        when(orderService.conditionSearch(any(OrderPageDTO.class))).thenReturn(pageResult);

        Result<PageResult> result = orderController.conditionSearch(pageDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(50L, result.getData().getTotal());
    }

    @Test
    @DisplayName("ORD-ADMIN-002: 测试订单统计")
    void testStatistics() {
        OrderStatisticsVO statisticsVO = OrderStatisticsVO.builder()
                .toBeConfirmed(10)
                .confirmed(5)
                .deliveryInProgress(3)
                .build();

        when(orderService.statistics()).thenReturn(statisticsVO);

        Result<OrderStatisticsVO> result = orderController.statistics();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(10, result.getData().getToBeConfirmed());
    }

    @Test
    @DisplayName("ORD-ADMIN-003: 测试查询订单详情")
    void testDetails() {
        OrderVO orderVO = new OrderVO();
        orderVO.setId(1);

        when(orderService.getById(1)).thenReturn(orderVO);

        Result<OrderVO> result = orderController.details(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getId());
    }

    @Test
    @DisplayName("ORD-ADMIN-004: 测试接单")
    void testConfirm() {
        OrderConfirmDTO confirmDTO = new OrderConfirmDTO();
        confirmDTO.setId(1);

        Result result = orderController.confirm(confirmDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).confirm(any(OrderConfirmDTO.class));
    }

    @Test
    @DisplayName("ORD-ADMIN-005: 测试拒单")
    void testReject() {
        OrderRejectionDTO rejectionDTO = new OrderRejectionDTO();
        rejectionDTO.setId(1);
        rejectionDTO.setRejectionReason("库存不足");

        Result result = orderController.reject(rejectionDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).reject(any(OrderRejectionDTO.class));
    }

    @Test
    @DisplayName("ORD-ADMIN-006: 测试取消订单")
    void testCancel() {
        OrderCancelDTO cancelDTO = new OrderCancelDTO();
        cancelDTO.setId(1);
        cancelDTO.setCancelReason("用户取消");

        Result result = orderController.cancel(cancelDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).cancel(any(OrderCancelDTO.class));
    }

    @Test
    @DisplayName("ORD-ADMIN-007: 测试派送订单")
    void testDelivery() {
        Result result = orderController.delivery(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).delivery(1);
    }

    @Test
    @DisplayName("ORD-ADMIN-008: 测试完成订单")
    void testComplete() {
        Result result = orderController.complete(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).complete(1);
    }
}