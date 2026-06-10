package fun.cyhgraph.init;

import fun.cyhgraph.entity.Employee;
import fun.cyhgraph.mapper.EmployeeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否存在cyh账号
        Employee existingEmployee = employeeMapper.getByAccount("cyh");
        if (existingEmployee == null) {
            // 创建cyh账号
            Employee employee = new Employee();
            employee.setName("超级管理员");
            employee.setAccount("cyh");
            // 密码加密
            String password = DigestUtils.md5DigestAsHex("123456".getBytes());
            employee.setPassword(password);
            employee.setPhone("13800138000");
            employee.setAge(25);
            employee.setGender(1);
            employee.setStatus(1);
            employee.setCreateUser(100);
            employee.setUpdateUser(100);
            
            // 保存到数据库
            employeeMapper.regEmployee(employee);
            log.info("已创建cyh超级管理员账号");
        } else {
            log.info("cyh账号已存在");
        }
    }
}
