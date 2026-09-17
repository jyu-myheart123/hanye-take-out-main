package fun.cyhgraph.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import fun.cyhgraph.entity.OrderDetail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 堂食开单价格试算/提交结果 VO
 * 小白讲解：开单页每次改菜品、填会员手机号、勾选积分抵扣时，后端都会把账算好返回：
 * 原价多少、活动减多少、会员折扣减多少、积分抵多少、最后实付多少，
 * 前端只负责照着展示，绝不自己算钱（防止算错或被人改价）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DineInPriceVO implements Serializable {

    // ===== 订单基础信息（提交成功后才有值，试算时为空）=====
    private Integer id;                 // 订单id
    private String orderNumber;         // 订单号
    private LocalDateTime orderTime;    // 下单时间

    // ===== 价格明细 =====
    private BigDecimal originalAmount;  // 优惠前原价合计
    private BigDecimal discountAmount;  // 营销活动优惠金额
    private BigDecimal memberDiscount;  // 会员等级折扣金额
    private BigDecimal pointsDeduction; // 积分抵扣金额
    private BigDecimal payAmount;       // 应付/实付金额
    private BigDecimal orderAmount;     // 同实付金额（兼容旧前端字段）

    // ===== 命中的营销活动 =====
    private Integer promotionId;        // 活动id
    private String promotionName;       // 活动名称
    private Integer promotionType;      // 活动类型 1满减 2折扣 3第二份半价 4买一送一
    private List<String> tips;          // 优惠说明（如"宫保鸡丁 第二份半价 优惠¥9.00"）

    // ===== 会员信息（识别到会员时才有值）=====
    private Integer memberId;           // 会员id
    private String memberName;          // 会员姓名
    private String memberPhone;         // 会员手机号
    private Integer memberLevel;        // 会员等级 1银卡 2金卡 3钻石
    private String memberLevelName;     // 等级名称
    private BigDecimal levelDiscount;   // 等级折扣 1/0.95/0.9
    private BigDecimal balance;         // 当前余额（支付前）
    private Integer points;             // 当前积分（抵扣前）
    private BigDecimal maxPointsDeduction; // 当前积分最多能抵多少钱
    private BigDecimal balanceAfter;    // 下单后余额（提交成功后返回）
    private Integer pointsAfter;        // 下单后积分（提交成功后返回：本单获得的积分已算入）
    private Integer pointsEarned;       // 本单获得的积分

    /**
     * 订单明细（后端算账时内部使用：提交订单直接落库；试算响应里也会带，前端可不使用）
     */
    private List<OrderDetail> details;
}
