package fun.cyhgraph.vo;

import fun.cyhgraph.entity.Member;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员展示 VO
 * 小白讲解：在会员信息基础上附带"等级名称"和"当前等级折扣"，列表直接展示用
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MemberVO extends Member implements Serializable {

    /**
     * 等级名称，如"金卡会员"
     */
    private String levelName;
    /**
     * 当前等级享受的折扣：1=不打折，0.95=95折
     */
    private BigDecimal discount;
}
