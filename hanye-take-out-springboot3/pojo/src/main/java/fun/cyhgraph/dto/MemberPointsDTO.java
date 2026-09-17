package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 人工调整会员积分 DTO
 * 小白讲解：顾客投诉、活动补偿等场景下，员工可以手动给会员加/减积分
 */
@Data
public class MemberPointsDTO implements Serializable {

    /**
     * 会员id
     */
    private Integer memberId;
    /**
     * 积分变动数量：正数=增加，负数=扣减
     */
    private Integer points;
    /**
     * 调整原因/备注
     */
    private String remark;
}
