package fun.cyhgraph.controller.user;

import fun.cyhgraph.entity.Cart;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("购物车控制器单元测试")
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @Test
    @DisplayName("CT-001: 测试添加菜品到购物车")
    void testAddToCart() {
        fun.cyhgraph.dto.CartDTO cartDTO = new fun.cyhgraph.dto.CartDTO();
        cartDTO.setDishId(1);
        
        Result result = cartController.add(cartDTO);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(cartService).add(cartDTO);
    }

    @Test
    @DisplayName("CT-002: 测试添加套餐到购物车")
    void testAddSetmealToCart() {
        fun.cyhgraph.dto.CartDTO cartDTO = new fun.cyhgraph.dto.CartDTO();
        cartDTO.setSetmealId(1);
        
        Result result = cartController.add(cartDTO);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(cartService).add(cartDTO);
    }

    @Test
    @DisplayName("CT-003: 测试减少购物车数量")
    void testSubFromCart() {
        fun.cyhgraph.dto.CartDTO cartDTO = new fun.cyhgraph.dto.CartDTO();
        cartDTO.setDishId(1);
        
        Result result = cartController.sub(cartDTO);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(cartService).sub(cartDTO);
    }

    @Test
    @DisplayName("CT-004: 测试获取购物车列表")
    void testGetCartList() {
        List<Cart> cartList = new ArrayList<>();
        Cart cart = new Cart();
        cart.setId(1);
        cart.setDishId(1);
        cart.setNumber(2);
        cartList.add(cart);
        
        when(cartService.getList()).thenReturn(cartList);
        
        Result<List<Cart>> result = cartController.getList();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("CT-005: 测试获取空购物车列表")
    void testGetEmptyCartList() {
        when(cartService.getList()).thenReturn(new ArrayList<>());
        
        Result<List<Cart>> result = cartController.getList();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("CT-006: 测试清空购物车")
    void testCleanCart() {
        Result result = cartController.cleanCart();
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(cartService).clean();
    }
}