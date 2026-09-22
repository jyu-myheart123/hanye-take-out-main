package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 惊喜盲盒实体
 * 小白讲解：商家创建一个固定价格的盲盒（如 9.9 元），里面放 3 个套餐选项。
 * 顾客先按盲盒价付款，然后从三张背面卡牌中选一张翻开，翻到哪个套餐就吃哪个套餐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlindBox implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 盲盒名称（如"9.9元惊喜盲盒"）
     */
    private String name;
    /**
     * 盲盒售价：固定价，先付款再抽卡
     */
    private BigDecimal price;
    /**
     * 盲盒封面图（OSS 地址，选填）
     */
    private String image;
    /**
     * 盲盒说明（如"一荤一素一饭，今天吃什么交给运气"）
     */
    private String description;
    /**
     * 状态：1启用 0停用
     */
    private Integer status;
    /**
     * 套餐选项列表（不是数据库字段！查询列表时由 Service 手动填充）
     */
    private List<BlindBoxOption> options;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
