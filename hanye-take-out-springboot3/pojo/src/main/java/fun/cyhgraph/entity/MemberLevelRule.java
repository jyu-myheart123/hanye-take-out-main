package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级规则实体
 * 小白讲解：规定"累计消费满多少元自动升到哪个等级、该等级吃饭打几折"，
 * 商家可以在会员页面自己调整，不用改代码。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLevelRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 等级：1银卡 2金卡 3钻石
     */
    private Integer level;
    /**
     * 等级名称，如"金卡会员"
     */
    private String levelName;
    /**
     * 升级门槛：累计消费达到该金额即可升到本等级
     */
    private BigDecimal thresholdAmount;
    /**
     * 等级折扣：1=不打折，0.95=95折（下单在活动价基础上再打折）
     */
    private BigDecimal discount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
