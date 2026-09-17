package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员充值 DTO
 * 小白讲解：员工在会员页点"充值"时提交的数据，比如充200元、额外赠送20元
 */
@Data
public class MemberRechargeDTO implements Serializable {

    /**
     * 会员id
     */
    private Integer memberId;
    /**
     * 充值本金（必须大于0）
     */
    private BigDecimal amount;
    /**
     * 赠送金额（如充200送20，这里填20；不送就填0或不填）
     */
    private BigDecimal gift;
}
