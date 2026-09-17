package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员流水实体（余额和积分的每一次变动都留痕）
 * 小白讲解：就像银行卡的交易明细，充值、消费、用积分抵扣都会记一条，
 * 会员页面"消费记录"查的就是这张表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 流水类型常量 =====
    public static final Integer TYPE_RECHARGE = 1;        // 充值
    public static final Integer TYPE_CONSUME = 2;         // 消费（扣余额）
    public static final Integer TYPE_REFUND = 3;          // 退款
    public static final Integer TYPE_POINTS_EARN = 4;     // 积分累计（消费送积分）
    public static final Integer TYPE_POINTS_DEDUCT = 5;   // 积分抵扣（下单用积分抵钱）
    public static final Integer TYPE_MANUAL_ADJUST = 6;   // 人工调整积分

    private Integer id;
    /**
     * 会员id
     */
    private Integer memberId;
    /**
     * 流水类型：1充值 2消费 3退款 4积分累计 5积分抵扣 6人工调整
     */
    private Integer type;
    /**
     * 余额变动金额：正数=入账（充值/退款），负数=扣减（消费）
     */
    private BigDecimal amount;
    /**
     * 积分变动数量：正数=增加，负数=减少
     */
    private Integer points;
    /**
     * 变动后的余额（方便直接展示，不用重新计算）
     */
    private BigDecimal balanceAfter;
    /**
     * 变动后的积分
     */
    private Integer pointsAfter;
    /**
     * 关联的订单id（充值/人工调整时为空）
     */
    private Integer orderId;
    /**
     * 备注说明
     */
    private String remark;
    private LocalDateTime createTime;
}
