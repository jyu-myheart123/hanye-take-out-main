package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 提交客户评价 DTO
 * 小白讲解：堂食订单完成后，员工照着顾客反馈代填的评价表单
 */
@Data
public class ReviewDTO implements Serializable {

    /**
     * 被评价的订单id（必填，一个订单只能评价一次）
     */
    private Integer orderId;
    /**
     * 评价人称呼（选填，如"王先生"）
     */
    private String customerName;
    /**
     * 菜品总体评分：1-5
     */
    private Integer dishScore;
    /**
     * 口味评分：1-5（选填）
     */
    private Integer tasteScore;
    /**
     * 服务态度评分：1-5（必填）
     */
    private Integer serviceScore;
    /**
     * 出餐速度评分：1-5（选填）
     */
    private Integer speedScore;
    /**
     * 好评内容（满意的地方，选填）
     */
    private String content;
    /**
     * 改进意见（希望改进的地方，选填）
     */
    private String suggestion;
}
