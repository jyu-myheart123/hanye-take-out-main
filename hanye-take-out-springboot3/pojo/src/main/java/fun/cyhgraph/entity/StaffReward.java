package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工赏罚流水实体
 * 小白讲解：员工身上的每一笔钱和积分都在这里留痕：
 * 类型1=顾客打赏小费；类型2=店主奖励（发奖金/加积分）；类型3=店主惩戒（扣钱/扣积分）。
 * 金额 amount 带符号：奖励/打赏是正数，惩戒是负数；北极星积分 starPoints 同理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffReward implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 员工id
     */
    private Integer employeeId;
    /**
     * 类型：1顾客打赏 2店主奖励 3店主惩戒
     */
    private Integer type;
    /**
     * 金额（打赏/奖励为正，惩戒为负）
     */
    private BigDecimal amount;
    /**
     * 北极星积分变动（奖励为正，惩戒为负）
     */
    private Integer starPoints;
    /**
     * 关联的服务评分id（顾客打赏时有值）
     */
    private Integer ratingId;
    /**
     * 关联订单id（选填）
     */
    private Integer orderId;
    /**
     * 奖惩原因/说明
     */
    private String reason;
    /**
     * 操作人员工id（店主奖惩时记录是谁操作的）
     */
    private Integer operatorId;
    private LocalDateTime createTime;
}
