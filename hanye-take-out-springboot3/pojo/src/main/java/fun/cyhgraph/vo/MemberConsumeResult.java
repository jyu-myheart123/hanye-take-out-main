package fun.cyhgraph.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员消费处理结果 VO
 * 小白讲解：堂食下单扣完余额、积完分、升完级后，把会员的最新状态带回去，
 * 开单成功弹窗里就能提示"余额还剩多少、积分还剩多少、本单返了多少积分"。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberConsumeResult implements Serializable {

    private Integer memberId;          // 会员id
    private Integer level;             // 最新等级 1银卡 2金卡 3钻石
    private String levelName;          // 最新等级名称
    private BigDecimal balanceAfter;   // 消费后余额
    private Integer pointsAfter;       // 消费后积分
    private Integer pointsEarned;      // 本单获得积分
    private Integer pointsUsed;        // 本单用掉多少积分抵扣
}
