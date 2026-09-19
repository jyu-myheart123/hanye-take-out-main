package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工服务评分提交 DTO
 * 小白讲解：员工在"服务评分"页照着顾客反馈填写并提交的数据。
 * 小费不在这里传——评分先提交，成功后才弹"是否打赏"的弹窗，走单独的打赏接口。
 */
@Data
public class StaffRatingDTO implements Serializable {

    /**
     * 被评价的员工id（必填）
     */
    private Integer employeeId;
    /**
     * 关联堂食订单id（选填）
     */
    private Integer orderId;
    /**
     * 桌号（选填）
     */
    private String tableNo;
    /**
     * 顾客称呼（选填）
     */
    private String customerName;
    /**
     * 服务态度 1-5 星（必填）
     */
    private Integer serviceScore;
    /**
     * 推荐指数 1-5 星（选填）
     */
    private Integer recommendScore;
    /**
     * 评价内容（选填）
     */
    private String content;
}
