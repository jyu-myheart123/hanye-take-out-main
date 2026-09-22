package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 原材料供应商实体
 * 小白讲解：给店里供货的商家信息：叫什么、找谁联系、电话多少、主营什么。
 * 到货准时率、合格率、月底应付金额不在这里存，由采购记录实时统计，避免数字对不上。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 供应商名称
     */
    private String name;
    /**
     * 联系人
     */
    private String contactPerson;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 地址
     */
    private String address;
    /**
     * 主营品类（如时令蔬菜、生鲜猪肉）
     */
    private String mainCategory;
    /**
     * 状态：1合作中 0停用
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
