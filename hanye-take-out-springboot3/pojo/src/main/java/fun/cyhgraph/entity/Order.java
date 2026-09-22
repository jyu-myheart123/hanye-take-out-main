package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;

    /**
     * 支付状态 0未支付 1已支付 2退款
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    private Integer id;
    private String number;  // 订单号
    private Integer status; // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
    private Integer userId; // 下单用户id
    private Integer addressBookId; // 地址id
    private LocalDateTime orderTime; // 下单时间
    private LocalDateTime checkoutTime; // 结账时间
    private Integer payMethod; // 支付方式 1微信，2支付宝
    private Integer payStatus; // 支付状态 0未支付 1已支付 2退款
    private BigDecimal amount; // 实收金额
    /**
     * 优惠前原价（所有菜品单价×份数之和，老订单默认0）
     */
    private BigDecimal originalAmount;
    /**
     * 营销活动优惠金额（满减/折扣/第二份半价/买一送一）
     */
    private BigDecimal discountAmount;
    /**
     * 会员等级优惠金额（银卡/金卡/钻石折扣）
     */
    private BigDecimal memberDiscount;
    /**
     * 积分抵扣金额（100积分抵5元）
     */
    private BigDecimal pointsDeduction;
    /**
     * 命中的营销活动id（没命中为空）
     */
    private Integer promotionId;
    /**
     * 命中的营销活动名称（冗余，订单详情直接展示）
     */
    private String promotionName;
    /**
     * 下单会员id（散客为空）
     */
    private Integer memberId;
    /**
     * 惊喜盲盒id（只有盲盒订单才有值）
     */
    private Integer blindBoxId;
    /**
     * 抽中的盲盒套餐id（只有盲盒订单才有值）
     */
    private Integer blindBoxOptionId;
    private String remark; // 备注
    private String userName; // 用户名
    private String phone; // 手机号
    private String address; // 地址
    private String consignee; // 收货人
    private String cancelReason; // 订单取消原因
    private String rejectionReason; // 订单拒绝原因
    private LocalDateTime cancelTime; // 订单取消时间
    private LocalDateTime estimatedDeliveryTime; // 预计送达时间
    private Integer deliveryStatus; // 配送状态  1立即送出  0选择具体时间
    private LocalDateTime deliveryTime; // 送达时间
    private int packAmount; // 打包费
    private int tablewareNumber; // 餐具数量
    private Integer tablewareStatus; // 餐具数量状态  1按餐量提供  0选择具体数量
    /**
     * 订单类型：1=外卖（微信小程序用户下单），2=堂食（员工在后台为到店客户代下单）
     * 小白讲解：用这个字段区分订单来源，堂食单没有收货地址、现场付款
     */
    private Integer orderType;
}
