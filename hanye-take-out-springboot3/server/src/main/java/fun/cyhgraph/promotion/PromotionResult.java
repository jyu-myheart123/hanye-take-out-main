package fun.cyhgraph.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 单个营销活动在本单上的计算结果
 * 小白讲解：告诉上层"这个活动给本单省了多少钱、优惠明细怎么展示给顾客看"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResult implements Serializable {

    /**
     * 活动id
     */
    private Integer promotionId;
    /**
     * 活动名称
     */
    private String name;
    /**
     * 活动类型 1满减 2折扣 3第二份半价 4买一送一
     */
    private Integer type;
    /**
     * 本单优惠总金额
     */
    private BigDecimal saving;
    /**
     * 优惠明细文案（如"全场8.8折 优惠¥6.60"、"宫保鸡丁 第二份半价 优惠¥9.00"）
     */
    private List<String> tips;
}
