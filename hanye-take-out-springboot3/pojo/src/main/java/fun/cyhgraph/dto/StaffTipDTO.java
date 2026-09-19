package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 顾客打赏小费 DTO
 * 小白讲解：评分提交成功后弹出"是否给小费"的弹窗，顾客选择给时提交：
 * ratingId 告诉后端是哪次评分的打赏，amount 是小费金额。
 */
@Data
public class StaffTipDTO implements Serializable {

    /**
     * 服务评分id
     */
    private Integer ratingId;
    /**
     * 小费金额（必须大于0）
     */
    private BigDecimal amount;
}
