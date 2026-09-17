package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户评价实体
 * 小白讲解：堂食订单"已完成"后，员工可以照着顾客的反馈代填一条评价：
 * 给菜品、口味、服务、出餐速度打星，再写好评和改进意见，后厨/管理者在后台查看并回复。
 * 一个订单最多一条评价（数据库 order_id 加了唯一索引）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 被评价的订单id
     */
    private Integer orderId;
    /**
     * 订单号（冗余存一份，列表展示时不用再查订单表）
     */
    private String orderNumber;
    /**
     * 桌号/客户称呼（冗余，方便后厨知道是哪桌的反馈）
     */
    private String tableNo;
    /**
     * 评价的会员id（散客为空）
     */
    private Integer memberId;
    /**
     * 评价人称呼（如"王先生"，散客可手填）
     */
    private String customerName;
    /**
     * 菜品总体评分：1-5 星
     */
    private Integer dishScore;
    /**
     * 口味评分：1-5 星
     */
    private Integer tasteScore;
    /**
     * 服务态度评分：1-5 星（必填）
     */
    private Integer serviceScore;
    /**
     * 出餐速度评分：1-5 星
     */
    private Integer speedScore;
    /**
     * 好评内容（满意的地方）
     */
    private String content;
    /**
     * 改进意见（希望改进的地方）
     */
    private String suggestion;
    /**
     * 商家回复
     */
    private String reply;
    /**
     * 状态：1展示 0隐藏（不当评价可隐藏）
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
