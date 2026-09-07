package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee implements Serializable {

    private Integer id;
    private String name;
    private String account;
    private String password;
    private String phone;
    private Integer age;
    private Integer gender;
    private String pic;
    private Integer status;
    /**
     * 角色：1=超级管理员，0=普通员工
     * 小白讲解：用数字区分身份，超管能增删员工，普通员工只能改自己的资料
     */
    private Integer role;
    private Integer createUser;
    private Integer updateUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
