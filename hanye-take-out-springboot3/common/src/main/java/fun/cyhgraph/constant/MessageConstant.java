package fun.cyhgraph.constant;

public class MessageConstant {

    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ALREADY_EXiST = "已存在";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String LOGIN_FAILED = "用户登录失败";
    public static final String ADDRESS_BOOK_IS_NULL = "地址为空";
    public static final String CART_IS_NULL = "购物车为空";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String UNKNOWN_ERROR = "未知错误";
    // ===== 权限相关提示 =====
    public static final String NOT_ADMIN = "权限不足，该操作仅超级管理员可执行";
    public static final String CAN_NOT_OPERATE_ADMIN = "无权操作超级管理员账号";
    public static final String CAN_NOT_OPERATE_OTHER = "普通员工只能修改自己的信息";
    public static final String DINE_IN_EMPTY = "开单清单为空，请先添加菜品";
    public static final String DISH_NOT_AVAILABLE = "部分菜品已停售或不存在，请刷新后重试";
}
