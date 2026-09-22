package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 原材料实体
 * 小白讲解：店里采购的每一样原料：叫什么、按什么单位买（斤/个/桶）、
 * 大概多少钱、属于哪一类、通常找哪家供应商。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 原材料名称（如猪五花肉、东北大米）
     */
    private String name;
    /**
     * 单位：斤 / 个 / 桶
     */
    private String unit;
    /**
     * 参考单价（元/单位），实际价以采购记录为准
     */
    private BigDecimal referencePrice;
    /**
     * 分类：肉类/蔬菜/水产/粮油/调料等
     */
    private String category;
    /**
     * 常用供应商id
     */
    private Integer supplierId;
    /**
     * 状态：1在用 0停用
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
