package fun.cyhgraph.service.serviceImpl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.constant.MessageConstant;
import fun.cyhgraph.context.BaseContext;
import fun.cyhgraph.dto.EmployeeDTO;
import fun.cyhgraph.dto.EmployeeFixPwdDTO;
import fun.cyhgraph.dto.EmployeeLoginDTO;
import fun.cyhgraph.dto.PageDTO;
import fun.cyhgraph.entity.Employee;
import fun.cyhgraph.exception.BaseException;
import fun.cyhgraph.exception.PasswordErrorException;
import fun.cyhgraph.exception.EmployeeNotFoundException;
import fun.cyhgraph.mapper.EmployeeMapper;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String account = employeeLoginDTO.getAccount();
        String password = employeeLoginDTO.getPassword();
        // 先查数据库，看是否存在该账号
        Employee employee = employeeMapper.getByAccount(account);
        if (employee == null){
            throw new EmployeeNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 再将前端传过来的密码进行MD5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        // 和之前存进数据库的加密的密码进行比对，看看是否一样，不一样要抛异常
        if (!password.equals(employee.getPassword())){
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        return employee;
    }

    /**
     * 注册/新增员工
     */
    public void register(EmployeeLoginDTO employeeLoginDTO) {
        // 先对用户的密码进行MD5加密，再存到数据库中
        String password = employeeLoginDTO.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        employeeLoginDTO.setPassword(password);

        Employee employee = new Employee();
        // 将userLoginDTO的属性拷贝到user中
        BeanUtils.copyProperties(employeeLoginDTO, employee);
        // 为user其他字段填充默认值(7-3=4个)
        employee.setName("员工");
        employee.setPhone("11111111111");
        employee.setAge(0);
        employee.setGender(1);
        employee.setStatus(1);
        employee.setCreateUser(100); // 100表示员工自己注册，此时还不能拿到BaseContext的currentId，只能用100这个数字表示自己了
        employee.setUpdateUser(100);
        employeeMapper.regEmployee(employee);
    }

    /**
     * 根据id获取员工信息
     * @return
     */
    public Employee getEmployeeById(Integer id) {
        Employee employee = employeeMapper.getById(id);
        return employee;
    }

    /**
     * 员工分页查询
     * @return
     */
    public PageResult employeePageList(PageDTO pageDTO) {
        // 传分页参数给PageHelper自动处理，会自动加上limit和count(*)返回分页结果和总记录数
        PageHelper.startPage(pageDTO.getPage(), pageDTO.getPageSize());
        Page<Employee> pagelist = employeeMapper.pageQuery(pageDTO);
        return new PageResult(pagelist.getTotal(), pagelist.getResult());
    }

    /**
     * 修改员工
     * 权限规则（后端硬性校验，前端按钮显隐只是辅助）：
     *   超级管理员(role=1)：可以修改任意员工的资料
     *   普通员工(role=0)：只能修改自己的资料，动别人的直接拒绝
     * @param employeeDTO
     */
    public void update(EmployeeDTO employeeDTO) {
        Employee current = getCurrentEmployee();
        boolean isAdmin = current.getRole() != null && current.getRole() == 1;
        if (!isAdmin && !current.getId().equals(employeeDTO.getId())) {
            // 普通员工想改别人的资料 -> 拒绝
            throw new BaseException(MessageConstant.CAN_NOT_OPERATE_OTHER);
        }
        // 缺少时间等字段，需要手动加入，否则Mapper里的autofill注解会为EmployeeDTO去setUpdateTime，然而根本没这个方法导致报错！
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employeeMapper.update(employee);
    }

    /**
     * 删除员工（彻底从数据库删除）
     * 权限规则（后端硬性校验）：
     *   超级管理员(role=1)：可以删除任意【普通员工】，但不能删除其他超级管理员，也不能删除自己
     *   普通员工(role=0)：只能删除【自己】的账号，删除别人会被拒绝
     * 小白讲解：删除是高风险操作，所以前后端都要校验，前端藏按钮只是界面体验，
     * 真正的安全靠这里——有人用 Postman 直接调接口也会被拦住
     */
    public void delete(Integer id) {
        Employee current = getCurrentEmployee();
        boolean isAdmin = current.getRole() != null && current.getRole() == 1;
        Employee target = employeeMapper.getById(id);
        if (target == null) {
            throw new EmployeeNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (isAdmin) {
            // 超管账号受保护，连超管自己都不能删超管账号，避免把所有管理员都删光
            if (target.getRole() != null && target.getRole() == 1) {
                throw new BaseException(MessageConstant.CAN_NOT_OPERATE_ADMIN);
            }
            // 也不能删除当前登录的自己（删了就没人能管系统了）
            if (id.equals(current.getId())) {
                throw new BaseException("不能删除当前登录账号");
            }
        } else {
            // 普通员工：只能删除自己的账号，删别人的一律拒绝
            if (!id.equals(current.getId())) {
                throw new BaseException(MessageConstant.CAN_NOT_OPERATE_OTHER);
            }
        }
        employeeMapper.delete(id);
    }

    /**
     * 根据id修改员工状态（启用/禁用）
     * 权限规则：仅超级管理员可操作；超级管理员账号受保护不可被禁用
     * @param id
     */
    public void onOff(Integer id) {
        checkAdmin();
        Employee target = employeeMapper.getById(id);
        if (target == null) {
            throw new EmployeeNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (target.getRole() != null && target.getRole() == 1) {
            throw new BaseException(MessageConstant.CAN_NOT_OPERATE_ADMIN);
        }
        employeeMapper.onOff(id);
    }

    /**
     * 管理员新增员工
     * 权限规则：仅超级管理员可新增（普通员工即使绕过前端直接调接口也会被这里拦下）
     * @param employeeDTO
     */
    public void addEmployee(EmployeeDTO employeeDTO) {
        checkAdmin();
        // 先对用户的密码进行MD5加密，再存到数据库中
        String password = employeeDTO.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        employeeDTO.setPassword(password);
        // 创建employee对象，将employeeDTO的属性拷贝到employee中
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        // 为user其他字段填充默认值
        employee.setStatus(1);
        employeeMapper.addEmployee(employee);
    }

    /**
     * 获取当前登录的员工对象
     * 小白讲解：JWT 拦截器已经把当前登录人的 id 存进了 ThreadLocal，
     * 这里拿 id 再查一次数据库，就能拿到最新的角色信息，避免 token 里信息过期
     */
    private Employee getCurrentEmployee() {
        Integer currentId = BaseContext.getCurrentId();
        return employeeMapper.getById(currentId);
    }

    /**
     * 超级管理员校验：当前登录人不是超管就直接抛异常
     * 小白讲解：这是后端的"门禁"，前端藏按钮只是让界面干净，
     * 真正的安全靠这里——就算有人用 Postman 直接调接口也会被拒绝
     */
    private void checkAdmin() {
        Employee current = getCurrentEmployee();
        if (current == null || current.getRole() == null || current.getRole() != 1) {
            throw new BaseException(MessageConstant.NOT_ADMIN);
        }
    }

    /**
     * 修改密码
     * @param employeeFixPwdDTO
     */
    public void fixPwd(EmployeeFixPwdDTO employeeFixPwdDTO) {
        String oldPwd = employeeFixPwdDTO.getOldPwd();
        // 将前端传过来的旧密码进行MD5加密
        oldPwd = DigestUtils.md5DigestAsHex(oldPwd.getBytes());
        // 根据id查询当前账号信息
        Integer id = BaseContext.getCurrentId();
        Employee employee = employeeMapper.getById(id);
        // 和之前存进数据库的加密的密码进行比对，看看是否一样，不一样要抛异常
        if (!oldPwd.equals(employee.getPassword())){
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 旧密码正确，将新密码加密后进行更新
        String newPwd = employeeFixPwdDTO.getNewPwd();
        String password = DigestUtils.md5DigestAsHex(newPwd.getBytes());
        employee.setPassword(password);
        employeeMapper.updatePwd(employee);
    }
}
