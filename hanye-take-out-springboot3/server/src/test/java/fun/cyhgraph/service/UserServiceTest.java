package fun.cyhgraph.service;

import fun.cyhgraph.dto.UserLoginDTO;
import fun.cyhgraph.entity.User;
import fun.cyhgraph.exception.LoginFailedException;
import fun.cyhgraph.mapper.UserMapper;
import fun.cyhgraph.properties.WeChatProperties;
import fun.cyhgraph.service.serviceImpl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private WeChatProperties weChatProperties;

    private UserServiceImpl userService;

    private UserLoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        userService.setUserMapper(userMapper);
        userService.setWeChatProperties(weChatProperties);
        
        loginDTO = new UserLoginDTO();
        loginDTO.setCode("test_code");
    }

    @Test
    @DisplayName("UC-001: 测试登录-使用测试code，新用户自动注册")
    void testWxLogin_WithTestCode_NewUser() {
        when(userMapper.getByOpenid("test_openid_123456")).thenReturn(null);
        
        User result = userService.wxLogin(loginDTO);
        
        assertNotNull(result);
        assertEquals("test_openid_123456", result.getOpenid());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("UC-002: 测试登录-使用测试code，用户已存在")
    void testWxLogin_WithTestCode_UserExists() {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setOpenid("test_openid_123456");
        when(userMapper.getByOpenid("test_openid_123456")).thenReturn(existingUser);
        
        User result = userService.wxLogin(loginDTO);
        
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("UC-003: 测试登录-code为空，应该抛出异常")
    void testWxLogin_EmptyCode() {
        loginDTO.setCode("");
        assertThrows(LoginFailedException.class, () -> userService.wxLogin(loginDTO));
    }

    @Test
    @DisplayName("UC-004: 测试登录-code为null，应该抛出异常")
    void testWxLogin_NullCode() {
        loginDTO.setCode(null);
        assertThrows(LoginFailedException.class, () -> userService.wxLogin(loginDTO));
    }

    @Test
    @DisplayName("UC-005: 测试根据ID查询用户")
    void testGetUserById() {
        User expectedUser = new User();
        expectedUser.setId(1);
        expectedUser.setOpenid("test_openid_123456");
        expectedUser.setName("测试用户");
        when(userMapper.getById(1)).thenReturn(expectedUser);
        
        User result = userService.getUser(1);
        
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("测试用户", result.getName());
    }

    @Test
    @DisplayName("UC-006: 测试根据ID查询用户-用户不存在")
    void testGetUserById_NotFound() {
        when(userMapper.getById(999)).thenReturn(null);
        
        User result = userService.getUser(999);
        
        assertNull(result);
    }
}