package fun.cyhgraph.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新增采购入库记录 DTO
 * 小白讲解：供应商送一次货就登记一条：哪家、什么料、多少量、什么价、
 * 准时吗、合格吗、什么时候到的。合计金额后端自动算（数量×单价），不信前端传值。
 */
@Data
public class PurchaseRecordDTO implements Serializable {

    /**
     * 供应商id
     */
    private Integer supplierId;
    /**
     * 原材料id
     */
    private Integer materialId;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 是否准时：1是 0否
     */
    private Integer isOnTime;
    /**
     * 是否合格：1是 0否
     */
    private Integer isQualified;
    /**
     * 到货时间（前端按"年-月-日 时:分:秒"传字符串）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseTime;
    /**
     * 备注
     */
    private String remark;
}
