package fun.cyhgraph.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 盲盒抽卡结果 VO
 * 小白讲解：后端随机抽完、订单生成后，把三张卡的内容一起返回：
 * wonIndex 告诉前端哪张是"命运套餐"（该卡正面朝上），另外两张可随后翻开看。
 */
@Data
public class BlindBoxDrawResultVO implements Serializable {

    /**
     * 生成的订单id
     */
    private Integer orderId;
    /**
     * 订单号
     */
    private String orderNumber;
    /**
     * 订单类型 2堂食 1外卖
     */
    private Integer orderType;
    /**
     * 抽中第几张卡（0~2），前端把这张翻成正面
     */
    private Integer wonIndex;
    /**
     * 三张卡牌（按顺序，每个是一个套餐）
     */
    private List<DrawnCard> cards;
    /**
     * 盲盒售价（实付）
     */
    private BigDecimal boxPrice;
    /**
     * 抽中套餐菜品原价合计
     */
    private BigDecimal originalAmount;
    /**
     * 盲盒优惠金额（原价−盲盒价）
     */
    private BigDecimal discountAmount;

    /**
     * 一张卡牌的内容（套餐）
     */
    @Data
    public static class DrawnCard implements Serializable {
        /**
         * 套餐选项id
         */
        private Integer optionId;
        /**
         * 套餐名（幸运套餐A）
         */
        private String optionName;
        /**
         * 菜品摘要
         */
        private String dishSummary;
        /**
         * 套餐内的菜品明细（名字、图片、单价，卡牌翻转后展示）
         */
        private List<CardDish> dishes;
    }

    /**
     * 卡牌上的一道菜
     */
    @Data
    public static class CardDish implements Serializable {
        private Integer dishId;
        private String name;
        private String pic;
        private BigDecimal price;
        private Integer number;
    }
}
