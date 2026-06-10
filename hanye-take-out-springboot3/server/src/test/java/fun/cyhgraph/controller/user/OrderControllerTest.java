package fun.cyhgraph.controller.user;

import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.OrderService;
import fun.cyhgraph.vo.OrderSubmitVO;
import fun.cyhgraph.vo.OrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单控制器单元测试")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    @DisplayName("ORD-001: 测试提交订单")
    void testSubmitOrder() throws Exception {
        fun.cyhgraph.dto.OrderSubmitDTO orderSubmitDTO = new fun.cyhgraph.dto.OrderSubmitDTO();
        
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(1);
        
        when(orderService.submit(orderSubmitDTO)).thenReturn(orderSubmitVO);
        
        Result<OrderSubmitVO> result = orderController.submit(orderSubmitDTO);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("ORD-002: 测试查询未支付订单数量")
    void testUnPayOrderCount() {
        when(orderService.unPayOrderCount()).thenReturn(3);
        
        Result<Integer> result = orderController.unPayOrderCount();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(3, result.getData());
    }

    @Test
    @DisplayName("ORD-003: 测试订单支付")
    void testPayment() throws Exception {
        fun.cyhgraph.dto.OrderPaymentDTO paymentDTO = new fun.cyhgraph.dto.OrderPaymentDTO();
        
        fun.cyhgraph.vo.OrderPaymentVO paymentVO = new fun.cyhgraph.vo.OrderPaymentVO();
        
        when(orderService.payment(paymentDTO)).thenReturn(paymentVO);
        
        Result<fun.cyhgraph.vo.OrderPaymentVO> result = orderController.payment(paymentDTO);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("ORD-004: 测试查询订单详情")
    void testGetOrderDetail() {
        OrderVO orderVO = new OrderVO();
        orderVO.setId(1);
        
        when(orderService.getById(1)).thenReturn(orderVO);
        
        Result<OrderVO> result = orderController.getById(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("ORD-005: 测试分页查询历史订单")
    void testHistoryOrders() {
        PageResult pageResult = new PageResult();
        pageResult.setTotal(10L);
        
        when(orderService.userPage(1, 10, null)).thenReturn(pageResult);
        
        Result<PageResult> result = orderController.page(1, 10, null);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(10L, result.getData().getTotal());
    }

    @Test
    @DisplayName("ORD-006: 测试取消订单")
    void testCancelOrder() throws Exception {
        Result result = orderController.cancel(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).userCancelById(1);
    }

    @Test
    @DisplayName("ORD-007: 测试再来一单")
    void testReOrder() {
        Result result = orderController.reOrder(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).reOrder(1);
    }

    @Test
    @DisplayName("ORD-008: 测试催单")
    void testReminder() {
        Result result = orderController.reminder(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(orderService).reminder(1);
    }
}