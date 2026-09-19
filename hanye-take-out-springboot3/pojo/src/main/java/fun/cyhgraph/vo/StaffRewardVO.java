package fun.cyhgraph.vo;

import fun.cyhgraph.entity.StaffReward;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 赏罚流水 VO
 * 小白讲解：流水表本身只存员工id，列表上想直接显示员工姓名，
 * 所以继承流水实体再加一个 employeeName 字段（一次查询连表带出来）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StaffRewardVO extends StaffReward implements Serializable {

    /**
     * 员工姓名（连表查询带出来）
     */
    private String employeeName;
}
