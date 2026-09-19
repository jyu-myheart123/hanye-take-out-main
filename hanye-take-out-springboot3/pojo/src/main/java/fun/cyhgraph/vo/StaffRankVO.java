package fun.cyhgraph.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 员工星光排行榜 VO（排行榜一行的数据）
 * 小白讲解：选择"今日/近3日/近7日/近一月"后，每个员工在这段时间内的统计结果：
 * 被打了多少次分、平均服务分、平均推荐分、攒了多少北极星积分、收到多少小费、
 * 店主奖了多少/罚了多少，平均分高的排在最前面，方便店主发周奖金。
 */
@Data
public class StaffRankVO implements Serializable {

    /**
     * 员工id
     */
    private Integer employeeId;
    /**
     * 员工姓名
     */
    private String employeeName;
    /**
     * 评价人数（这段时间被打了多少次分）
     */
    private Integer ratingCount;
    /**
     * 平均服务态度分（5分制，保留两位小数）
     */
    private BigDecimal avgService;
    /**
     * 平均推荐指数
     */
    private BigDecimal avgRecommend;
    /**
     * 北极星积分合计（每次评分几星积几分，再加上店主的奖惩积分）
     */
    private Integer starPoints;
    /**
     * 收到的小费合计
     */
    private BigDecimal tipTotal;
    /**
     * 店主奖惩净额（奖励为正、惩戒为负）
     */
    private BigDecimal rewardNet;
}
