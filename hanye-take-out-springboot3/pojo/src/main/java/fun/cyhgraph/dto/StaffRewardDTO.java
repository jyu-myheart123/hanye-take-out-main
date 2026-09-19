package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 店主奖励/惩戒 DTO
 * 小白讲解：店主在排行榜上点"奖励"或"惩戒"时填写：
 * 选哪个员工、是奖还是罚、多少钱、加减多少北极星积分、原因是什么。
 * 注意：金额这里都传正数，后端会根据 type 自动把惩戒的钱/积分存成负数。
 */
@Data
public class StaffRewardDTO implements Serializable {

    /**
     * 员工id
     */
    private Integer employeeId;
    /**
     * 类型：2店主奖励 3店主惩戒（打赏是顾客走的，不允许手动选1）
     */
    private Integer type;
    /**
     * 金额（正数；惩戒时后端自动转负）
     */
    private BigDecimal amount;
    /**
     * 北极星积分（正数；惩戒时后端自动转负，可为0表示只动钱）
     */
    private Integer starPoints;
    /**
     * 奖惩原因
     */
    private String reason;
}
