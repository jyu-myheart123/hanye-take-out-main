package fun.cyhgraph.vo;

import fun.cyhgraph.entity.PurchaseRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 采购记录 VO（采购记录列表中的一行）
 * 小白讲解：采购记录本身只存供应商id、原材料id，列表展示时连表把
 * 供应商名称、原料名称和单位带出来，不用前端再逐个查。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseRecordVO extends PurchaseRecord implements Serializable {

    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 原材料名称
     */
    private String materialName;
    /**
     * 原材料单位
     */
    private String materialUnit;
}
