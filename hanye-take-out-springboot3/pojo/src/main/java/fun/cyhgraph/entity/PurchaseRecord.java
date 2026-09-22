package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购入库记录实体
 * 小白讲解：每次供应商送货就登记一条：哪家供应商、什么原料、买了多少、单价多少、
 * 合计多少钱、有没有准时到、验收合不合格。今日/本月采购统计和月底对账都靠它。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 供应商id
     */
    private Integer supplierId;
    /**
     * 原材料id
     */
    private Integer materialId;
    /**
     * 采购数量
     */
    private BigDecimal quantity;
    /**
     * 实际单价
     */
    private BigDecimal unitPrice;
    /**
     * 合计金额 = 数量 × 单价
     */
    private BigDecimal totalAmount;
    /**
     * 是否准时到货：1是 0否（统计准时率用）
     */
    private Integer isOnTime;
    /**
     * 是否验收合格：1是 0否（统计合格率用）
     */
    private Integer isQualified;
    /**
     * 采购/到货时间
     */
    private LocalDateTime purchaseTime;
    /**
     * 备注
     */
    private String remark;
    private LocalDateTime createTime;
}
