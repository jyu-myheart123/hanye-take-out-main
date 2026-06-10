package fun.cyhgraph.controller.user;

import fun.cyhgraph.dto.UserLoginDTO;
import fun.cyhgraph.entity.User;
import fun.cyhgraph.properties.JwtProperties;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.UserService;
import fun.cyhgraph.vo.UserLoginVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户控制器单元测试")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Test
    @DisplayName("UC-CTL-001: 测试登录接口-成功")
    void testLogin_Success() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setCode("test_code");
        
        User user = new User();
        user.setId(1);
        user.setOpenid("test_openid_123456");
        
        when(userService.wxLogin(any(UserLoginDTO.class))).thenReturn(user);
        
        UserController controller = new UserController();
        controller.setUserService(userService);
        
        JwtProperties jwtProps = new JwtProperties();
        jwtProps.setUserSecretKey("test-secret-key");
        jwtProps.setUserTtl(3600000L);
        controller.setJwtProperties(jwtProps);
        
        Result<UserLoginVO> result = controller.login(loginDTO);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getId());
        assertEquals("test_openid_123456", result.getData().getOpenid());
        assertNotNull(result.getData().getToken());
    }

    @Test
    @DisplayName("UC-CTL-003: 测试根据ID查询用户-成功")
    void testGetUser_Success() {
        User expectedUser = new User();
        expectedUser.setId(1);
        expectedUser.setOpenid("test_openid_123456");
        expectedUser.setName("测试用户");
        expectedUser.setPhone("13800138000");
        
        when(userService.getUser(1)).thenReturn(expectedUser);
        
        UserController controller = new UserController();
        controller.setUserService(userService);
        
        Result<User> result = controller.getUser(1);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getId());
        assertEquals("测试用户", result.getData().getName());
    }

    @Test
    @DisplayName("UC-CTL-004: 测试根据ID查询用户-用户不存在")
    void testGetUser_NotFound() {
        when(userService.getUser(999)).thenReturn(null);
        
        UserController controller = new UserController();
        controller.setUserService(userService);
        
        Result<User> result = controller.getUser(999);
        
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("UC-CTL-005: 测试修改用户信息-成功")
    void testUpdate_Success() {
        UserController controller = new UserController();
        controller.setUserService(userService);
        
        fun.cyhgraph.dto.UserDTO userDTO = new fun.cyhgraph.dto.UserDTO();
        userDTO.setId(1);
        userDTO.setName("新用户名");
        userDTO.setPhone("13900139000");
        
        assertDoesNotThrow(() -> controller.update(userDTO));
        verify(userService).update(any(fun.cyhgraph.dto.UserDTO.class));
    }
}