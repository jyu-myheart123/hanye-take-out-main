package fun.cyhgraph.vo;

import fun.cyhgraph.entity.OrderDetail;
import fun.cyhgraph.entity.Review;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价展示 VO
 * 小白讲解：评价内容 + 这单点了哪些菜、消费多少钱，后厨看评价时一目了然
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReviewVO extends Review implements Serializable {

    /**
     * 下单时间
     */
    private LocalDateTime orderTime;
    /**
     * 订单实付金额
     */
    private BigDecimal orderAmount;
    /**
     * 订单菜品拼成的字符串，如"宫保鸡丁*2;米饭*1;"
     */
    private String orderDishes;
    /**
     * 订单菜品明细
     */
    private List<OrderDetail> orderDetailList;
}
