package fun.cyhgraph.vo;

import fun.cyhgraph.entity.Supplier;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 供应商统计 VO（供应商列表中的一行）
 * 小白讲解：供应商基本信息 + 本月送货单数/金额 + 准时率/合格率，
 * 统计数字由采购记录实时聚合，不额外存储，保证永远对得上。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierStatVO extends Supplier implements Serializable {

    /**
     * 本月送货单数
     */
    private Integer monthCount;
    /**
     * 本月送货金额（月底应付参考）
     */
    private BigDecimal monthAmount;
    /**
     * 到货准时率（百分比，如 80.00 表示 80%）
     */
    private BigDecimal onTimeRate;
    /**
     * 验收合格率（百分比）
     */
    private BigDecimal qualifiedRate;
}
