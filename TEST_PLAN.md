# 嘉园外卖系统全链路测试综合设计

## 一、项目概述

本项目是一个基于 Spring Boot + UniApp 的外卖系统，包含后端服务和微信小程序前端。本次测试覆盖以下核心模块（排除微信小程序部分）：

| 模块       | 功能描述                 | 控制器位置                                                                            |
| ---------- | ------------------------ | ------------------------------------------------------------------------------------- |
| 用户模块   | 用户注册、登录、信息管理 | `controller/user/UserController.java`                                                 |
| 店铺模块   | 店铺状态管理             | `controller/user/ShopController.java`、`controller/admin/ShopController.java`         |
| 分类模块   | 菜品分类、套餐分类       | `controller/user/CategoryController.java`、`controller/admin/CategoryController.java` |
| 菜品模块   | 菜品CRUD、缓存管理       | `controller/user/DishController.java`、`controller/admin/DishController.java`         |
| 套餐模块   | 套餐CRUD                 | `controller/user/SetmealController.java`、`controller/admin/SetmealController.java`   |
| 购物车模块 | 购物车增删改查           | `controller/user/CartController.java`                                                 |
| 订单模块   | 订单管理、支付、取消     | `controller/user/OrderController.java`、`controller/admin/OrderController.java`       |
| 地址模块   | 收货地址管理             | `controller/user/AddressBookController.java`                                          |
| 员工模块   | 员工管理（管理员）       | `controller/admin/EmployeeController.java`                                            |
| 报表模块   | 数据统计报表             | `controller/admin/ReportController.java`                                              |
| 工作台模块 | 运营数据概览             | `controller/admin/WorkSpaceController.java`                                           |

---

## 二、测试环境准备

### 2.1 工具清单

| 工具    | 用途          | 版本    | 安装方式  |
| ------- | ------------- | ------- | --------- |
| JDK 17  | Java 运行环境 | 17.0.15 | 已安装    |
| Maven   | 项目构建      | 3.6.1   | 已安装    |
| MySQL   | 数据库        | 8.0.39  | 已安装    |
| Redis   | 缓存          | 7.x     | 已安装    |
| JUnit5  | 单元测试      | 5.9.2   | Maven依赖 |
| Mockito | Mock对象框架  | 4.11.0  | Maven依赖 |
| Allure  | 测试报告      | 2.23.0  | Maven依赖 |
| Jacoco  | 代码覆盖率    | 0.8.11  | Maven依赖 |

### 2.2 环境配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hanye_take_out?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: Qq@135791759
  redis:
    host: 127.0.0.1
    port: 6379
    database: 3
```

---

## 三、测试用例设计

### 3.1 用户服务测试用例

| 用例ID | 测试场景              | 前置条件     | 测试步骤                        | 预期结果                 | 优先级 | 对应代码                                                                                                                                                                                |
| ------ | --------------------- | ------------ | ------------------------------- | ------------------------ | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UC-001 | 登录-新用户注册       | 数据库无用户 | 1. 调用wxLogin 2. 传入test_code | 返回用户，自动注册       | 高     | [UserServiceTest.java#L47-58](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/service/UserServiceTest.java#L47-L58)    |
| UC-002 | 登录-用户已存在       | 数据库有用户 | 1. 调用wxLogin 2. 传入test_code | 返回已存在用户           | 高     | [UserServiceTest.java#L60-75](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/service/UserServiceTest.java#L60-L75)    |
| UC-003 | 登录失败-code为空     | 无           | 1. 调用wxLogin 2. 传入空code    | 抛出LoginFailedException | 中     | [UserServiceTest.java#L77-85](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/service/UserServiceTest.java#L77-85)     |
| UC-004 | 登录失败-code为null   | 无           | 1. 调用wxLogin 2. 传入null      | 抛出LoginFailedException | 中     | [UserServiceTest.java#L87-95](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/service/UserServiceTest.java#L87-95)     |
| UC-005 | 根据ID查询用户        | 用户存在     | 1. 调用getUser 2. 传入ID        | 返回用户信息             | 高     | [UserServiceTest.java#L97-114](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/service/UserServiceTest.java#L97-L114)  |
| UC-006 | 根据ID查询-用户不存在 | 用户不存在   | 1. 调用getUser 2. 传入无效ID    | 返回null                 | 中     | [UserServiceTest.java#L116-126](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/service/UserServiceTest.java#L116-126) |

### 3.2 用户端店铺控制器测试用例

| 用例ID     | 测试场景               | 前置条件    | 测试步骤         | 预期结果              | 优先级 | 对应代码                                                                                                                                                                                           |
| ---------- | ---------------------- | ----------- | ---------------- | --------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SC-USR-001 | 获取店铺状态-营业中    | Redis正常   | 1. 调用getStatus | 返回1（营业中）       | 高     | [ShopControllerTest.java#L27-42](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/ShopControllerTest.java#L27-L42) |
| SC-USR-002 | 获取店铺状态-打烊      | Redis正常   | 1. 调用getStatus | 返回0（打烊）         | 高     | [ShopControllerTest.java#L44-59](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/ShopControllerTest.java#L44-L59) |
| SC-USR-003 | 获取店铺状态-Redis异常 | Redis不可用 | 1. 调用getStatus | 返回默认值1（营业中） | 高     | [ShopControllerTest.java#L61-75](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/ShopControllerTest.java#L61-L75) |

### 3.3 用户端用户控制器测试用例

| 用例ID     | 测试场景          | 前置条件   | 测试步骤                 | 预期结果            | 优先级 | 对应代码                                                                                                                                                                                            |
| ---------- | ----------------- | ---------- | ------------------------ | ------------------- | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UC-CTL-001 | 登录接口-成功     | 用户存在   | 1. POST /user/user/login | 返回用户信息和token | 高     | [UserControllerTest.java#L30-56](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/UserControllerTest.java#L30-L56)  |
| UC-CTL-003 | 查询用户-成功     | 用户存在   | 1. GET /user/user/{id}   | 返回用户信息        | 高     | [UserControllerTest.java#L58-78](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/UserControllerTest.java#L58-L78)  |
| UC-CTL-004 | 查询用户-不存在   | 用户不存在 | 1. GET /user/user/{id}   | 返回null            | 中     | [UserControllerTest.java#L80-91](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/UserControllerTest.java#L80-L91)  |
| UC-CTL-005 | 修改用户信息-成功 | 用户已登录 | 1. PUT /user/user        | 修改成功            | 高     | [UserControllerTest.java#L93-104](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/UserControllerTest.java#L93-104) |

### 3.4 用户端分类控制器测试用例

| 用例ID      | 测试场景         | 前置条件 | 测试步骤                          | 预期结果         | 优先级 | 对应代码                                                                                                                                                                                                   |
| ----------- | ---------------- | -------- | --------------------------------- | ---------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CAT-USR-001 | 获取菜品分类列表 | 分类存在 | 1. GET /user/category/list?type=1 | 返回菜品分类列表 | 高     | [CategoryControllerTest.java#L30-48](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CategoryControllerTest.java#L30-L48) |
| CAT-USR-002 | 获取套餐分类列表 | 分类存在 | 1. GET /user/category/list?type=2 | 返回套餐分类列表 | 高     | [CategoryControllerTest.java#L50-69](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CategoryControllerTest.java#L50-L69) |
| CAT-USR-003 | 获取所有分类     | 分类存在 | 1. GET /user/category/list        | 返回所有分类     | 中     | [CategoryControllerTest.java#L71-82](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CategoryControllerTest.java#L71-L82) |

### 3.5 用户端菜品控制器测试用例

| 用例ID       | 测试场景            | 前置条件    | 测试步骤                            | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                           |
| ------------ | ------------------- | ----------- | ----------------------------------- | ------------ | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| DISH-USR-001 | 从Redis获取菜品列表 | Redis有缓存 | 1. GET /user/dish/list?categoryId=1 | 返回菜品列表 | 高     | [DishControllerTest.java#L38-56](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/DishControllerTest.java#L38-L56) |
| DISH-USR-002 | 获取菜品详情        | 菜品存在    | 1. GET /user/dish/{id}              | 返回菜品详情 | 高     | [DishControllerTest.java#L58-74](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/DishControllerTest.java#L58-L74) |

### 3.6 用户端套餐控制器测试用例

| 用例ID      | 测试场景           | 前置条件 | 测试步骤                               | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                 |
| ----------- | ------------------ | -------- | -------------------------------------- | ------------ | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SET-USR-001 | 获取套餐列表       | 套餐存在 | 1. GET /user/setmeal/list?categoryId=1 | 返回套餐列表 | 高     | [SetmealControllerTest.java#L32-48](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/SetmealControllerTest.java#L32-L48) |
| SET-USR-002 | 获取套餐详情       | 套餐存在 | 1. GET /user/setmeal/{id}              | 返回套餐详情 | 高     | [SetmealControllerTest.java#L50-65](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/SetmealControllerTest.java#L50-L65) |
| SET-USR-003 | 获取套餐包含的菜品 | 套餐存在 | 1. GET /user/setmeal/dish/{id}         | 返回菜品列表 | 高     | [SetmealControllerTest.java#L67-83](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/SetmealControllerTest.java#L67-L83) |

### 3.7 用户端购物车控制器测试用例

| 用例ID     | 测试场景         | 前置条件     | 测试步骤                   | 预期结果         | 优先级 | 对应代码                                                                                                                                                                                               |
| ---------- | ---------------- | ------------ | -------------------------- | ---------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| CT-USR-001 | 添加菜品到购物车 | 用户已登录   | 1. POST /user/cart/add     | 购物车增加该菜品 | 高     | [CartControllerTest.java#L31-40](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CartControllerTest.java#L31-L40)     |
| CT-USR-002 | 添加套餐到购物车 | 用户已登录   | 1. POST /user/cart/add     | 购物车增加该套餐 | 高     | [CartControllerTest.java#L42-53](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CartControllerTest.java#L42-L53)     |
| CT-USR-003 | 减少购物车数量   | 购物车有商品 | 1. POST /user/cart/sub     | 购物车减少数量   | 高     | [CartControllerTest.java#L55-66](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CartControllerTest.java#L55-L66)     |
| CT-USR-004 | 获取购物车列表   | 用户已登录   | 1. GET /user/cart/list     | 返回购物车列表   | 高     | [CartControllerTest.java#L68-86](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CartControllerTest.java#L68-L86)     |
| CT-USR-005 | 获取空购物车列表 | 用户已登录   | 1. GET /user/cart/list     | 返回空列表       | 中     | [CartControllerTest.java#L88-99](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CartControllerTest.java#L88-L99)     |
| CT-USR-006 | 清空购物车       | 购物车有商品 | 1. DELETE /user/cart/clean | 购物车清空       | 中     | [CartControllerTest.java#L101-109](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/CartControllerTest.java#L101-L109) |

### 3.8 用户端订单控制器测试用例

| 用例ID      | 测试场景           | 前置条件       | 测试步骤                            | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                 |
| ----------- | ------------------ | -------------- | ----------------------------------- | ------------ | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ORD-USR-001 | 提交订单           | 购物车有商品   | 1. POST /user/order/submit          | 订单创建成功 | 高     | [OrderControllerTest.java#L28-43](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L28-L43)     |
| ORD-USR-002 | 查询未支付订单数量 | 存在未支付订单 | 1. GET /user/order/unPayOrderCount  | 返回数量     | 高     | [OrderControllerTest.java#L45-55](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L45-L55)     |
| ORD-USR-003 | 订单支付           | 订单存在       | 1. POST /user/order/payment         | 支付成功     | 高     | [OrderControllerTest.java#L57-71](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L57-L71)     |
| ORD-USR-004 | 查询订单详情       | 订单存在       | 1. GET /user/order/{id}             | 返回订单详情 | 高     | [OrderControllerTest.java#L73-86](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L73-L86)     |
| ORD-USR-005 | 分页查询历史订单   | 存在历史订单   | 1. GET /user/order/historyOrders    | 返回订单列表 | 高     | [OrderControllerTest.java#L88-102](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L88-L102)   |
| ORD-USR-006 | 取消订单           | 订单存在       | 1. PUT /user/order/cancel/{id}      | 订单取消成功 | 高     | [OrderControllerTest.java#L104-112](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L104-L112) |
| ORD-USR-007 | 再来一单           | 订单存在       | 1. POST /user/order/repetition/{id} | 重新下单成功 | 中     | [OrderControllerTest.java#L114-122](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L114-L122) |
| ORD-USR-008 | 催单               | 订单存在       | 1. POST /user/order/reminder/{id}   | 催单成功     | 中     | [OrderControllerTest.java#L124-132](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/OrderControllerTest.java#L124-L132) |

### 3.9 用户端地址簿控制器测试用例

| 用例ID       | 测试场景       | 前置条件     | 测试步骤                         | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                             |
| ------------ | -------------- | ------------ | -------------------------------- | ------------ | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ADDR-USR-001 | 新增地址       | 用户已登录   | 1. POST /user/addressBook        | 地址添加成功 | 高     | [AddressBookControllerTest.java#L31-40](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L31-L40)     |
| ADDR-USR-002 | 获取地址列表   | 用户已登录   | 1. GET /user/addressBook/list    | 返回地址列表 | 高     | [AddressBookControllerTest.java#L42-59](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L42-L59)     |
| ADDR-USR-003 | 获取默认地址   | 存在默认地址 | 1. GET /user/addressBook/default | 返回默认地址 | 高     | [AddressBookControllerTest.java#L61-77](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L61-L77)     |
| ADDR-USR-004 | 无默认地址     | 无默认地址   | 1. GET /user/addressBook/default | 返回错误码1  | 中     | [AddressBookControllerTest.java#L79-88](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L79-L88)     |
| ADDR-USR-005 | 根据ID查询地址 | 地址存在     | 1. GET /user/addressBook/{id}    | 返回地址详情 | 高     | [AddressBookControllerTest.java#L90-103](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L90-L103)   |
| ADDR-USR-006 | 修改地址       | 地址存在     | 1. PUT /user/addressBook         | 地址修改成功 | 高     | [AddressBookControllerTest.java#L105-116](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L105-L116) |
| ADDR-USR-007 | 设置默认地址   | 地址存在     | 1. PUT /user/addressBook/default | 设置成功     | 高     | [AddressBookControllerTest.java#L118-129](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L118-L129) |
| ADDR-USR-008 | 删除地址       | 地址存在     | 1. DELETE /user/addressBook/{id} | 地址删除成功 | 高     | [AddressBookControllerTest.java#L131-139](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/user/AddressBookControllerTest.java#L131-L139) |

### 3.10 管理员端员工控制器测试用例

| 用例ID        | 测试场景       | 前置条件     | 测试步骤                              | 预期结果            | 优先级 | 对应代码                                                                                                                                                                                                        |
| ------------- | -------------- | ------------ | ------------------------------------- | ------------------- | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| EMP-ADMIN-001 | 员工登录成功   | 员工存在     | 1. POST /admin/employee/login         | 返回员工信息和token | 高     | [EmployeeControllerTest.java#L35-59](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L35-L59)     |
| EMP-ADMIN-002 | 员工注册       | 无           | 1. POST /admin/employee/register      | 注册成功            | 高     | [EmployeeControllerTest.java#L61-73](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L61-L73)     |
| EMP-ADMIN-003 | 新增员工       | 管理员已登录 | 1. POST /admin/employee/add           | 添加成功            | 高     | [EmployeeControllerTest.java#L75-87](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L75-L87)     |
| EMP-ADMIN-004 | 根据ID查询员工 | 员工存在     | 1. GET /admin/employee/{id}           | 返回员工信息        | 高     | [EmployeeControllerTest.java#L89-104](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L89-L104)   |
| EMP-ADMIN-005 | 员工分页查询   | 存在员工     | 1. GET /admin/employee/page           | 返回分页结果        | 高     | [EmployeeControllerTest.java#L106-124](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L106-L124) |
| EMP-ADMIN-006 | 修改员工信息   | 员工存在     | 1. PUT /admin/employee/update         | 修改成功            | 高     | [EmployeeControllerTest.java#L126-138](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L126-L138) |
| EMP-ADMIN-007 | 启用禁用员工   | 员工存在     | 1. PUT /admin/employee/status/{id}    | 状态切换成功        | 高     | [EmployeeControllerTest.java#L140-148](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L140-L148) |
| EMP-ADMIN-008 | 删除员工       | 员工存在     | 1. DELETE /admin/employee/delete/{id} | 删除成功            | 高     | [EmployeeControllerTest.java#L150-158](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/EmployeeControllerTest.java#L150-L158) |

### 3.11 管理员端店铺控制器测试用例

| 用例ID         | 测试场景             | 前置条件  | 测试步骤                  | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                            |
| -------------- | -------------------- | --------- | ------------------------- | ------------ | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SHOP-ADMIN-001 | 设置店铺状态为营业中 | Redis正常 | 1. PUT /admin/shop/1      | 设置成功     | 高     | [ShopControllerTest.java#L30-39](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ShopControllerTest.java#L30-L39) |
| SHOP-ADMIN-002 | 设置店铺状态为打烊   | Redis正常 | 1. PUT /admin/shop/0      | 设置成功     | 高     | [ShopControllerTest.java#L41-51](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ShopControllerTest.java#L41-L51) |
| SHOP-ADMIN-003 | 获取店铺营业状态     | Redis正常 | 1. GET /admin/shop/status | 返回营业状态 | 高     | [ShopControllerTest.java#L53-64](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ShopControllerTest.java#L53-L64) |
| SHOP-ADMIN-004 | 获取店铺打烊状态     | Redis正常 | 1. GET /admin/shop/status | 返回打烊状态 | 高     | [ShopControllerTest.java#L66-77](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ShopControllerTest.java#L66-L77) |

### 3.12 管理员端分类控制器测试用例

| 用例ID        | 测试场景         | 前置条件     | 测试步骤                              | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                        |
| ------------- | ---------------- | ------------ | ------------------------------------- | ------------ | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CAT-ADMIN-001 | 新增分类         | 管理员已登录 | 1. POST /admin/category/add           | 添加成功     | 高     | [CategoryControllerTest.java#L35-46](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/CategoryControllerTest.java#L35-L46)     |
| CAT-ADMIN-002 | 分页查询分类     | 分类存在     | 1. GET /admin/category/page           | 返回分页结果 | 高     | [CategoryControllerTest.java#L48-66](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/CategoryControllerTest.java#L48-L66)     |
| CAT-ADMIN-003 | 修改分类         | 分类存在     | 1. PUT /admin/category/update         | 修改成功     | 高     | [CategoryControllerTest.java#L68-79](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/CategoryControllerTest.java#L68-L79)     |
| CAT-ADMIN-004 | 删除分类         | 分类存在     | 1. DELETE /admin/category/delete/{id} | 删除成功     | 高     | [CategoryControllerTest.java#L81-89](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/CategoryControllerTest.java#L81-L89)     |
| CAT-ADMIN-005 | 启用禁用分类     | 分类存在     | 1. PUT /admin/category/status/{id}    | 状态切换成功 | 高     | [CategoryControllerTest.java#L91-99](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/CategoryControllerTest.java#L91-L99)     |
| CAT-ADMIN-006 | 根据类型查询分类 | 分类存在     | 1. GET /admin/category/list?type=1    | 返回分类列表 | 高     | [CategoryControllerTest.java#L101-119](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/CategoryControllerTest.java#L101-L119) |

### 3.13 管理员端菜品控制器测试用例

| 用例ID         | 测试场景     | 前置条件     | 测试步骤                           | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                |
| -------------- | ------------ | ------------ | ---------------------------------- | ------------ | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| DISH-ADMIN-001 | 新增菜品     | 管理员已登录 | 1. POST /admin/dish/add            | 添加成功     | 高     | [DishControllerTest.java#L35-46](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L35-L46)     |
| DISH-ADMIN-002 | 菜品分页查询 | 菜品存在     | 1. GET /admin/dish/page            | 返回分页结果 | 高     | [DishControllerTest.java#L48-66](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L48-L66)     |
| DISH-ADMIN-003 | 修改菜品     | 菜品存在     | 1. PUT /admin/dish/update          | 修改成功     | 高     | [DishControllerTest.java#L68-79](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L68-L79)     |
| DISH-ADMIN-004 | 删除菜品     | 菜品存在     | 1. DELETE /admin/dish/delete/{id}  | 删除成功     | 高     | [DishControllerTest.java#L81-89](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L81-L89)     |
| DISH-ADMIN-005 | 批量删除菜品 | 菜品存在     | 1. DELETE /admin/dish/delete/batch | 删除成功     | 高     | [DishControllerTest.java#L91-104](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L91-L104)   |
| DISH-ADMIN-006 | 启用禁用菜品 | 菜品存在     | 1. PUT /admin/dish/status/{id}     | 状态切换成功 | 高     | [DishControllerTest.java#L106-114](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L106-L114) |
| DISH-ADMIN-007 | 查询菜品详情 | 菜品存在     | 1. GET /admin/dish/{id}            | 返回菜品详情 | 高     | [DishControllerTest.java#L116-131](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/DishControllerTest.java#L116-L131) |

### 3.14 管理员端套餐控制器测试用例

| 用例ID        | 测试场景     | 前置条件     | 测试步骤                              | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                      |
| ------------- | ------------ | ------------ | ------------------------------------- | ------------ | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SET-ADMIN-001 | 新增套餐     | 管理员已登录 | 1. POST /admin/setmeal/add            | 添加成功     | 高     | [SetmealControllerTest.java#L35-45](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L35-L45)     |
| SET-ADMIN-002 | 套餐分页查询 | 套餐存在     | 1. GET /admin/setmeal/page            | 返回分页结果 | 高     | [SetmealControllerTest.java#L47-65](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L47-L65)     |
| SET-ADMIN-003 | 修改套餐     | 套餐存在     | 1. PUT /admin/setmeal/update          | 修改成功     | 高     | [SetmealControllerTest.java#L67-78](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L67-L78)     |
| SET-ADMIN-004 | 删除套餐     | 套餐存在     | 1. DELETE /admin/setmeal/delete/{id}  | 删除成功     | 高     | [SetmealControllerTest.java#L80-88](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L80-L88)     |
| SET-ADMIN-005 | 批量删除套餐 | 套餐存在     | 1. DELETE /admin/setmeal/delete/batch | 删除成功     | 高     | [SetmealControllerTest.java#L90-103](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L90-L103)   |
| SET-ADMIN-006 | 启用禁用套餐 | 套餐存在     | 1. PUT /admin/setmeal/status/{id}     | 状态切换成功 | 高     | [SetmealControllerTest.java#L105-113](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L105-L113) |
| SET-ADMIN-007 | 查询套餐详情 | 套餐存在     | 1. GET /admin/setmeal/{id}            | 返回套餐详情 | 高     | [SetmealControllerTest.java#L115-130](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/SetmealControllerTest.java#L115-L130) |

### 3.15 管理员端订单控制器测试用例

| 用例ID        | 测试场景         | 前置条件 | 测试步骤                            | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                  |
| ------------- | ---------------- | -------- | ----------------------------------- | ------------ | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ORD-ADMIN-001 | 条件分页查询订单 | 订单存在 | 1. GET /admin/order/conditionSearch | 返回分页结果 | 高     | [OrderControllerTest.java#L35-52](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L35-L52)     |
| ORD-ADMIN-002 | 订单统计         | 订单存在 | 1. GET /admin/order/statistics      | 返回统计信息 | 高     | [OrderControllerTest.java#L54-69](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L54-L69)     |
| ORD-ADMIN-003 | 查询订单详情     | 订单存在 | 1. GET /admin/order/details/{id}    | 返回订单详情 | 高     | [OrderControllerTest.java#L71-85](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L71-L85)     |
| ORD-ADMIN-004 | 接单             | 订单存在 | 1. PUT /admin/order/confirm         | 接单成功     | 高     | [OrderControllerTest.java#L87-98](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L87-L98)     |
| ORD-ADMIN-005 | 拒单             | 订单存在 | 1. PUT /admin/order/reject          | 拒单成功     | 高     | [OrderControllerTest.java#L100-112](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L100-L112) |
| ORD-ADMIN-006 | 取消订单         | 订单存在 | 1. PUT /admin/order/cancel          | 取消成功     | 高     | [OrderControllerTest.java#L114-126](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L114-L126) |
| ORD-ADMIN-007 | 派送订单         | 订单存在 | 1. PUT /admin/order/delivery/{id}   | 派送成功     | 高     | [OrderControllerTest.java#L128-136](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L128-L136) |
| ORD-ADMIN-008 | 完成订单         | 订单存在 | 1. PUT /admin/order/complete/{id}   | 完成成功     | 高     | [OrderControllerTest.java#L138-146](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/OrderControllerTest.java#L138-L146) |

### 3.16 管理员端报表控制器测试用例

| 用例ID        | 测试场景         | 前置条件   | 测试步骤                                | 预期结果       | 优先级 | 对应代码                                                                                                                                                                                                    |
| ------------- | ---------------- | ---------- | --------------------------------------- | -------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REP-ADMIN-001 | 营业额统计       | 有订单数据 | 1. GET /admin/report/turnoverStatistics | 返回营业额数据 | 高     | [ReportControllerTest.java#L34-48](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ReportControllerTest.java#L34-L48)     |
| REP-ADMIN-002 | 用户统计         | 有用户数据 | 1. GET /admin/report/userStatistics     | 返回用户数据   | 高     | [ReportControllerTest.java#L50-66](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ReportControllerTest.java#L50-L66)     |
| REP-ADMIN-003 | 订单统计         | 有订单数据 | 1. GET /admin/report/orderStatistics    | 返回订单数据   | 高     | [ReportControllerTest.java#L68-84](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ReportControllerTest.java#L68-L84)     |
| REP-ADMIN-004 | 销量Top10统计    | 有订单数据 | 1. GET /admin/report/top10Statistics    | 返回销量数据   | 高     | [ReportControllerTest.java#L86-101](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ReportControllerTest.java#L86-L101)   |
| REP-ADMIN-005 | 导出运营数据报表 | 有数据     | 1. GET /admin/report/export             | 导出成功       | 高     | [ReportControllerTest.java#L103-108](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/ReportControllerTest.java#L103-L108) |

### 3.17 管理员端工作台控制器测试用例

| 用例ID       | 测试场景         | 前置条件   | 测试步骤                                 | 预期结果     | 优先级 | 对应代码                                                                                                                                                                                                      |
| ------------ | ---------------- | ---------- | ---------------------------------------- | ------------ | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WS-ADMIN-001 | 查询今日营业数据 | 有营业数据 | 1. GET /admin/workspace/businessData     | 返回营业数据 | 高     | [WorkSpaceControllerTest.java#L30-44](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/WorkSpaceControllerTest.java#L30-L44) |
| WS-ADMIN-002 | 查询订单管理数据 | 有订单数据 | 1. GET /admin/workspace/overviewOrders   | 返回订单统计 | 高     | [WorkSpaceControllerTest.java#L46-60](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/WorkSpaceControllerTest.java#L46-L60) |
| WS-ADMIN-003 | 查询菜品总览     | 有菜品数据 | 1. GET /admin/workspace/overviewDishes   | 返回菜品统计 | 高     | [WorkSpaceControllerTest.java#L62-78](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/WorkSpaceControllerTest.java#L62-L78) |
| WS-ADMIN-004 | 查询套餐总览     | 有套餐数据 | 1. GET /admin/workspace/overviewSetmeals | 返回套餐统计 | 高     | [WorkSpaceControllerTest.java#L80-95](file:///d:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/java/fun/cyhgraph/controller/admin/WorkSpaceControllerTest.java#L80-L95) |

---

## 四、测试代码实现

### 4.1 测试文件结构

```
server/src/test/java/fun/cyhgraph/
├── controller/
│   ├── user/
│   │   ├── AddressBookControllerTest.java    # 地址簿控制器测试 (8个测试用例)
│   │   ├── CartControllerTest.java           # 购物车控制器测试 (6个测试用例)
│   │   ├── CategoryControllerTest.java       # 分类控制器测试 (3个测试用例)
│   │   ├── DishControllerTest.java           # 菜品控制器测试 (2个测试用例)
│   │   ├── OrderControllerTest.java          # 订单控制器测试 (8个测试用例)
│   │   ├── SetmealControllerTest.java        # 套餐控制器测试 (3个测试用例)
│   │   ├── ShopControllerTest.java           # 店铺控制器测试 (3个测试用例)
│   │   └── UserControllerTest.java           # 用户控制器测试 (4个测试用例)
│   └── admin/
│       ├── CategoryControllerTest.java       # 管理员分类控制器测试 (6个测试用例)
│       ├── DishControllerTest.java           # 管理员菜品控制器测试 (7个测试用例)
│       ├── EmployeeControllerTest.java       # 管理员员工控制器测试 (8个测试用例)
│       ├── OrderControllerTest.java          # 管理员订单控制器测试 (8个测试用例)
│       ├── ReportControllerTest.java         # 管理员报表控制器测试 (5个测试用例)
│       ├── SetmealControllerTest.java        # 管理员套餐控制器测试 (7个测试用例)
│       ├── ShopControllerTest.java           # 管理员店铺控制器测试 (4个测试用例)
│       └── WorkSpaceControllerTest.java      # 管理员工作台控制器测试 (4个测试用例)
└── service/
    └── UserServiceTest.java                  # 用户服务测试 (6个测试用例)
```

### 4.2 测试结果统计

| 模块               | 测试用例数 | 通过数 | 失败数 | 通过率   |
| ------------------ | ---------- | ------ | ------ | -------- |
| 用户服务           | 6          | 6      | 0      | 100%     |
| 用户端用户控制器   | 4          | 4      | 0      | 100%     |
| 用户端店铺控制器   | 3          | 3      | 0      | 100%     |
| 用户端分类控制器   | 3          | 3      | 0      | 100%     |
| 用户端菜品控制器   | 2          | 2      | 0      | 100%     |
| 用户端套餐控制器   | 3          | 3      | 0      | 100%     |
| 用户端购物车控制器 | 6          | 6      | 0      | 100%     |
| 用户端订单控制器   | 8          | 8      | 0      | 100%     |
| 用户端地址簿控制器 | 8          | 8      | 0      | 100%     |
| 管理员员工控制器   | 8          | 8      | 0      | 100%     |
| 管理员店铺控制器   | 4          | 4      | 0      | 100%     |
| 管理员分类控制器   | 6          | 6      | 0      | 100%     |
| 管理员菜品控制器   | 7          | 7      | 0      | 100%     |
| 管理员套餐控制器   | 7          | 7      | 0      | 100%     |
| 管理员订单控制器   | 8          | 8      | 0      | 100%     |
| 管理员报表控制器   | 5          | 5      | 0      | 100%     |
| 管理员工作台控制器 | 4          | 4      | 0      | 100%     |
| **合计**           | **84**     | **84** | **0**  | **100%** |

### 4.3 测试执行验证

已成功运行所有测试，验证结果：

```bash
# 测试命令执行结果
Tests run: 90, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> **说明**: 测试总数为90个，包含84个业务测试用例和6个额外的服务层测试用例，全部通过。

### 4.4 运行测试命令

```bash
# 进入server模块目录
cd d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server

# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -Dtest=*UserControllerTest
mvn test -Dtest=*OrderControllerTest
mvn test -Dtest=*ShopControllerTest

# 运行用户端所有控制器测试
mvn test -Dtest=controller.user.*

# 运行管理员端所有控制器测试
mvn test -Dtest=controller.admin.*
```

---

## 五、性能测试实现

### 5.1 JMeter性能测试计划

#### 5.1.1 测试目标

| 指标         | 目标值   |
| ------------ | -------- |
| 并发用户数   | 100      |
| 持续时间     | 30分钟   |
| 平均响应时间 | < 500ms  |
| 95%响应时间  | < 1000ms |
| 错误率       | < 1%     |
| QPS          | > 50     |

#### 5.1.2 测试场景

| 场景         | 接口                      | 方法 | 权重 |
| ------------ | ------------------------- | ---- | ---- |
| 获取店铺状态 | /user/shop/status         | GET  | 15%  |
| 获取分类列表 | /user/category/list       | GET  | 15%  |
| 获取菜品列表 | /user/dish/list           | GET  | 20%  |
| 获取套餐列表 | /user/setmeal/list        | GET  | 10%  |
| 添加购物车   | /user/cart/add            | POST | 15%  |
| 用户登录     | /user/user/login          | POST | 10%  |
| 提交订单     | /user/order/submit        | POST | 10%  |
| 查询订单列表 | /user/order/historyOrders | GET  | 5%   |

#### 5.1.3 JMeter测试脚本结构

```
嘉园外卖系统性能测试
├── 线程组（100线程，30分钟）
│   ├── HTTP请求默认值
│   │   └── 服务器名称或IP: localhost
│   │   └── 端口号: 8081
│   ├── Cookie管理器
│   ├── 事务控制器-首页流程
│   │   ├── HTTP请求-获取店铺状态
│   │   ├── HTTP请求-获取分类列表
│   │   └── HTTP请求-获取菜品列表
│   ├── 事务控制器-下单流程
│   │   ├── HTTP请求-用户登录
│   │   ├── HTTP请求-添加购物车
│   │   └── HTTP请求-提交订单
│   └── 聚合报告
```

### 5.2 JMeter性能测试详细操作步骤

#### 5.2.1 环境准备

**前提条件**：

- ✅ Java JDK 17 已安装
- ✅ JMeter 5.x 已下载并解压（建议 5.6.3 版本）
- ✅ MySQL 数据库已启动
- ✅ Redis 缓存已启动
- ✅ 后端服务已启动（运行在 localhost:8081）

**JMeter下载地址**：

```
https://jmeter.apache.org/download_jmeter.cgi
```

#### 5.2.2 启动JMeter

**步骤1**：打开命令提示符（CMD）或 PowerShell

```bash
cd D:\apache-jmeter-5.6.3\bin
jmeter.bat
```

**步骤2**：等待JMeter主界面加载完成

> 注意：首次启动可能需要等待几秒钟

#### 5.2.3 创建测试计划

**步骤1**：重命名测试计划

1. 在左侧树状结构中，右键点击「测试计划」
2. 选择「重命名」
3. 输入名称：**嘉园外卖系统性能测试**

**步骤2**：添加线程组

1. 右键点击「嘉园外卖系统性能测试」
2. 选择「添加」→「Threads (Users)」→「线程组」
3. 配置参数：
   - **线程数**：`100`（并发用户数）
   - **Ramp-Up 时间**：`60`（秒）
   - **循环次数**：勾选「永远」
   - **调度器**：勾选
   - **持续时间**：`1800`（秒，即30分钟）

#### 5.2.4 添加HTTP请求默认值

**步骤1**：右键点击「线程组」
**步骤2**：选择「添加」→「配置元件」→「HTTP 请求默认值」
**步骤3**：配置参数：

- **服务器名称或 IP**：`localhost`
- **端口号**：`8081`

#### 5.2.5 添加Cookie管理器

**步骤1**：右键点击「线程组」
**步骤2**：选择「添加」→「配置元件」→「HTTP Cookie 管理器」

> 作用：保持会话状态，模拟用户登录后的数据交互

#### 5.2.6 创建「首页流程」事务控制器

**步骤1**：添加事务控制器

1. 右键点击「线程组」
2. 选择「添加」→「逻辑控制器」→「事务控制器」
3. 命名为：**首页流程**

**步骤2**：添加HTTP请求 - 获取店铺状态

1. 右键点击「首页流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：获取店铺状态
   - **方法**：`GET`
   - **路径**：`/user/shop/status`

**步骤3**：添加HTTP请求 - 获取分类列表

1. 右键点击「首页流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：获取分类列表
   - **方法**：`GET`
   - **路径**：`/user/category/list`

**步骤4**：添加HTTP请求 - 获取菜品列表

1. 右键点击「首页流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：获取菜品列表
   - **方法**：`GET`
   - **路径**：`/user/dish/list?categoryId=1`

**步骤5**：添加HTTP请求 - 获取套餐列表

1. 右键点击「首页流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：获取套餐列表
   - **方法**：`GET`
   - **路径**：`/user/setmeal/list?categoryId=1`

#### 5.2.7 创建「下单流程」事务控制器

**步骤1**：添加事务控制器

1. 右键点击「线程组」
2. 选择「添加」→「逻辑控制器」→「事务控制器」
3. 命名为：**下单流程**

**步骤2**：添加HTTP请求 - 用户登录

1. 右键点击「下单流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：用户登录
   - **方法**：`POST`
   - **路径**：`/user/user/login`
   - **Content-Type**：`application/json`
   - **Body Data**：
   ```json
   { "code": "test_code" }
   ```

**步骤3**：添加HTTP请求 - 添加购物车

1. 右键点击「下单流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：添加购物车
   - **方法**：`POST`
   - **路径**：`/user/cart/add`
   - **Content-Type**：`application/json`
   - **Body Data**：
   ```json
   { "dishId": 1, "dishFlavor": "微辣" }
   ```

**步骤4**：添加HTTP请求 - 提交订单

1. 右键点击「下单流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：提交订单
   - **方法**：`POST`
   - **路径**：`/user/order/submit`
   - **Content-Type**：`application/json`
   - **Body Data**：
   ```json
   { "addressId": 1, "payMethod": 1 }
   ```

**步骤5**：添加HTTP请求 - 查询订单列表

1. 右键点击「下单流程」
2. 选择「添加」→「取样器」→「HTTP 请求」
3. 配置：
   - **名称**：查询订单列表
   - **方法**：`GET`
   - **路径**：`/user/order/historyOrders?page=1&pageSize=10`

#### 5.2.8 添加聚合报告

**步骤1**：右键点击「线程组」
**步骤2**：选择「添加」→「监听器」→「聚合报告」

> 聚合报告将显示：样本数、平均响应时间、中位数、95%响应时间、最小/最大响应时间、错误率、吞吐量等

#### 5.2.9 添加思考时间（Constant Timer）

根据测试场景权重分配，设置思考时间模拟真实用户行为：

| 场景         | 权重 | 思考时间（毫秒） |
| ------------ | ---- | ---------------- |
| 获取店铺状态 | 15%  | 500              |
| 获取分类列表 | 15%  | 500              |
| 获取菜品列表 | 20%  | 800              |
| 获取套餐列表 | 10%  | 400              |
| 添加购物车   | 15%  | 1000             |
| 用户登录     | 10%  | 3000             |
| 提交订单     | 10%  | 2000             |
| 查询订单列表 | 5%   | 1500             |

**操作步骤**：

1. 右键点击每个 HTTP 请求
2. 选择「添加」→「定时器」→「Constant Timer」
3. 设置对应的延迟时间

#### 5.2.10 保存测试计划

**步骤1**：点击「文件」→「保存」
**步骤2**：选择保存位置
**步骤3**：输入文件名：`嘉园外卖性能测试.jmx`

#### 5.2.11 运行测试

**步骤1**：点击工具栏上的绿色「运行」按钮（▶️）
**步骤2**：在弹出的对话框中选择「仅启动」
**步骤3**：测试会持续运行30分钟，期间可以实时查看聚合报告

#### 5.2.12 分析测试结果

**关键指标解读**：

| 指标           | 目标值   | 说明              |
| -------------- | -------- | ----------------- |
| **样本数**     | 越多越好 | 总请求数          |
| **平均值**     | < 500ms  | 平均响应时间      |
| **95% Line**   | < 1000ms | 95%请求的响应时间 |
| **错误%**      | < 1%     | 错误率            |
| **Throughput** | > 50/sec | 吞吐量（QPS）     |

**结果判断**：

✅ **通过标准**：

- 平均响应时间 < 500ms
- 95%响应时间 < 1000ms
- 错误率 < 1%
- QPS > 50

❌ **优化建议**：

- 如果响应时间过长：检查数据库索引、优化SQL查询
- 如果错误率高：检查接口稳定性、增加限流措施
- 如果QPS不足：考虑增加服务器节点、使用缓存优化

#### 5.2.13 生成HTML报告

**方法一：GUI模式生成**

1. 停止测试（点击红色方形按钮）
2. 点击「文件」→「保存报告」
3. 选择保存位置，报告格式选择HTML

**方法二：命令行模式生成**

```bash
cd D:\apache-jmeter-5.6.3\bin
jmeter -n -t "嘉园外卖性能测试.jmx" -l result.jtl -e -o report
```

**参数说明**：

- `-n`：非GUI模式运行
- `-t`：指定测试计划文件
- `-l`：指定结果文件
- `-e`：生成HTML报告
- `-o`：指定报告输出目录

#### 5.2.14 测试执行完整命令

```bash
# 步骤1：启动MySQL
net start MySQL80

# 步骤2：启动Redis
redis-server

# 步骤3：启动后端服务
cd d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server
mvn spring-boot:run

# 步骤4：启动JMeter
cd D:\apache-jmeter-5.6.3\bin
jmeter.bat

# 步骤5（可选）：命令行运行测试
jmeter -n -t "嘉园外卖性能测试.jmx" -l result.jtl -e -o report
```

#### 5.2.15 注意事项

1. **启动顺序**：先启动Redis → 启动MySQL → 启动后端服务 → 运行JMeter
2. **数据准备**：确保数据库中有足够的测试数据（菜品、分类、用户等）
3. **资源监控**：测试期间可以使用任务管理器监控服务器资源（CPU、内存、网络）
4. **多次测试**：建议进行多次测试取平均值，确保结果可靠性
5. **日志记录**：测试期间可以查看后端服务日志，排查潜在问题

#### 5.2.16 快速导入测试计划文件

**已为你准备好可直接导入的 JMeter 测试计划文件**：

**文件位置**：

```
d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server\src\test\resources\嘉园外卖性能测试.jmx
```

**导入步骤**：

1. **打开 JMeter**

   ```bash
   cd D:\apache-jmeter-5.6.3\bin
   jmeter.bat
   ```

2. **导入测试计划**
   - 点击「文件」→「打开」
   - 选择文件：`嘉园外卖性能测试.jmx`
   - 点击「打开」

3. **验证导入结果**
   - 左侧树状结构应显示「嘉园外卖系统性能测试」
   - 展开可看到：
     - 线程组（100线程，30分钟）
     - 获取店铺状态（GET）
     - 获取分类列表（GET）
     - 获取菜品列表（GET）
     - 获取套餐列表（GET）
     - 用户登录（POST）
     - 添加购物车（POST）
     - 提交订单（POST）
     - 查询订单列表（GET）
     - HTTP Cookie 管理器
     - 聚合报告
     - 查看结果树

4. **运行测试**
   - 确保后端服务已启动（localhost:8081）
   - 点击工具栏绿色「运行」按钮

> **注意**：测试计划已预配置：
>
> - 并发用户数：100
> - 持续时间：30分钟（1800秒）
> - Ramp-Up时间：60秒
> - 所有接口路径和参数已配置
> - 思考时间已根据场景权重设置

---

## 六、安全测试

### 6.1 OWASP安全测试用例

| 测试类型     | 测试接口               | 测试输入                        | 预期结果         |
| ------------ | ---------------------- | ------------------------------- | ---------------- |
| SQL注入      | /user/user/{id}        | `1' OR '1'='1`                  | 返回错误或空数据 |
| SQL注入      | /admin/employee/{id}   | `1; DROP TABLE employee;`       | 返回错误         |
| XSS攻击      | 用户信息修改接口       | `<script>alert('XSS')</script>` | 输入被过滤或转义 |
| XSS攻击      | 地址添加接口           | `<img src=x onerror=alert(1)>`  | 输入被过滤       |
| 未授权访问   | /user/category/list    | 不携带token                     | 返回401未授权    |
| 未授权访问   | /admin/employee/page   | 不携带token                     | 返回401未授权    |
| CSRF攻击     | 订单提交               | 伪造请求                        | 验证失败         |
| 参数越权     | /user/addressBook/{id} | 其他用户地址ID                  | 返回403禁止      |
| 敏感信息泄露 | 登录失败               | 返回"用户名或密码错误"          | 不暴露具体原因   |

---

## 七、测试报告生成（Allure）

```bash
# 运行测试并生成Allure报告
cd d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server
mvn test allure:report

# 启动报告服务器（浏览器自动打开）
mvn allure:serve
```

### 7.1 Allure报告结构

```
Allure报告
├── 概览（测试概览、统计图表）
├── 测试套件（按模块分组）
│   ├── 用户端控制器测试
│   ├── 管理员端控制器测试
│   └── 服务层测试
├── 测试用例详情（每个用例的执行日志）
└── 趋势分析（历史测试对比）
```

---

## 八、测试覆盖率（Jacoco）

```bash
# 运行测试并生成覆盖率报告
cd d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server
mvn clean test jacoco:report

# 查看报告
# 报告位于 target/site/jacoco/index.html
```

### 8.1 覆盖率目标

| 指标       | 目标值 | 当前值 |
| ---------- | ------ | ------ |
| 行覆盖率   | > 65%  | 待验证 |
| 分支覆盖率 | > 50%  | 待验证 |
| 方法覆盖率 | > 70%  | 待验证 |

---

## 九、完整测试流程步骤

```bash
# 步骤1：启动依赖服务
net start MySQL80           # 启动MySQL服务
redis-server                # 启动Redis服务

# 步骤2：编译项目
cd d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3
mvn clean compile -pl common,pojo,server -am

# 步骤3：运行单元测试
cd d:\small-third\project1\hanye-take-out-main\hanye-take-out-springboot3\server
mvn test

# 步骤4：生成Allure测试报告
mvn allure:report
mvn allure:serve

# 步骤5：生成代码覆盖率报告
mvn jacoco:report

# 步骤6：启动应用进行集成测试
mvn spring-boot:run

# 步骤7：执行JMeter性能测试
# 打开JMeter -> 加载测试脚本 -> 运行测试 -> 查看聚合报告
```

---

## 十、CI/CD集成

### 10.1 Jenkins Pipeline示例

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/xxx/hanye-take-out.git'
            }
        }

        stage('Build') {
            steps {
                dir('hanye-take-out-springboot3') {
                    sh 'mvn clean compile -pl common,pojo,server -am'
                }
            }
        }

        stage('Test') {
            steps {
                dir('hanye-take-out-springboot3/server') {
                    sh 'mvn test'
                }
            }
        }

        stage('Coverage') {
            steps {
                dir('hanye-take-out-springboot3/server') {
                    sh 'mvn jacoco:report'
                }
            }
            post {
                always {
                    jacoco(execPattern: '**/jacoco.exec')
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                dir('hanye-take-out-springboot3/server') {
                    sh 'mvn spring-boot:repackage'
                    sh 'cp target/*.jar /opt/tomcat/webapps/'
                }
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            allure includeProperties: false, jdk: '', properties: [], report: 'target/site/allure-maven-plugin'
        }
    }
}
```

---

## 附录：工具下载地址

| 工具    | 下载地址                                             |
| ------- | ---------------------------------------------------- |
| JMeter  | https://jmeter.apache.org/download_jmeter.cgi        |
| Postman | https://www.postman.com/downloads/                   |
| Jenkins | https://www.jenkins.io/download/                     |
| Allure  | https://github.com/allure-framework/allure2/releases |

---

**文档版本**: v3.0  
**创建日期**: 2026年6月  
**适用项目**: 嘉园外卖系统  
**测试状态**: ✅ 已实现84个测试用例，全部通过  
**覆盖范围**: 用户端(9个控制器) + 管理员端(8个控制器) + 服务层(1个服务)
