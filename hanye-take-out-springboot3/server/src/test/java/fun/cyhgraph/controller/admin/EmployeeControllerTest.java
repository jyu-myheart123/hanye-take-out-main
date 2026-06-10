package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.EmployeeDTO;
import fun.cyhgraph.dto.EmployeeLoginDTO;
import fun.cyhgraph.dto.PageDTO;
import fun.cyhgraph.entity.Employee;
import fun.cyhgraph.properties.JwtProperties;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.EmployeeService;
import fun.cyhgraph.vo.EmployeeLoginVO;
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
@DisplayName("管理员员工控制器单元测试")
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private EmployeeController employeeController;

    @Test
    @DisplayName("EMP-ADMIN-001: 测试员工登录成功")
    void testLogin_Success() {
        EmployeeLoginDTO loginDTO = new EmployeeLoginDTO();
        loginDTO.setAccount("admin");
        loginDTO.setPassword("123456");

        Employee employee = new Employee();
        employee.setId(1);
        employee.setAccount("admin");

        when(employeeService.login(any(EmployeeLoginDTO.class))).thenReturn(employee);
        when(jwtProperties.getEmployeeSecretKey()).thenReturn("test-secret-key");
        when(jwtProperties.getEmployeeTtl()).thenReturn(3600000L);

        Result<EmployeeLoginVO> result = employeeController.login(loginDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getId());
        assertNotNull(result.getData().getToken());
    }

    @Test
    @DisplayName("EMP-ADMIN-002: 测试员工注册")
    void testRegister() {
        EmployeeLoginDTO loginDTO = new EmployeeLoginDTO();
        loginDTO.setAccount("newuser");
        loginDTO.setPassword("123456");

        Result result = employeeController.register(loginDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(employeeService).register(any(EmployeeLoginDTO.class));
    }

    @Test
    @DisplayName("EMP-ADMIN-003: 测试新增员工")
    void testAddEmployee() {
        EmployeeDTO employeeDTO = new EmployeeDTO();
        employeeDTO.setName("测试员工");
        employeeDTO.setAccount("testuser");

        Result result = employeeController.addEmployee(employeeDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(employeeService).addEmployee(any(EmployeeDTO.class));
    }

    @Test
    @DisplayName("EMP-ADMIN-004: 测试根据ID查询员工")
    void testGetEmployeeById() {
        Employee employee = new Employee();
        employee.setId(1);
        employee.setName("测试员工");

        when(employeeService.getEmployeeById(1)).thenReturn(employee);

        Result<Employee> result = employeeController.getEmployeeById(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("测试员工", result.getData().getName());
    }

    @Test
    @DisplayName("EMP-ADMIN-005: 测试员工分页查询")
    void testEmployeePageList() {
        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(1);
        pageDTO.setPageSize(10);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(100L);

        when(employeeService.employeePageList(any(PageDTO.class))).thenReturn(pageResult);

        Result<PageResult> result = employeeController.employeePageList(pageDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(100L, result.getData().getTotal());
    }

    @Test
    @DisplayName("EMP-ADMIN-006: 测试修改员工信息")
    void testUpdateEmployee() {
        EmployeeDTO employeeDTO = new EmployeeDTO();
        employeeDTO.setId(1);
        employeeDTO.setName("新名字");

        Result result = employeeController.update(employeeDTO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(employeeService).update(any(EmployeeDTO.class));
    }

    @Test
    @DisplayName("EMP-ADMIN-007: 测试启用禁用员工")
    void testOnOffEmployee() {
        Result result = employeeController.onOff(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(employeeService).onOff(1);
    }

    @Test
    @DisplayName("EMP-ADMIN-008: 测试删除员工")
    void testDeleteEmployee() {
        Result result = employeeController.delete(1);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        verify(employeeService).delete(1);
    }
}