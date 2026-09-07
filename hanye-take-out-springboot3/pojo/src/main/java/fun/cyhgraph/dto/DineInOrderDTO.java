package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 堂食下单 DTO
 * 小白讲解：老客户到店堂食时，员工在后台帮客户开单用的数据结构。
 * 和外卖下单不同：没有收货地址、没有在线支付，员工只需要勾选菜品并填写桌号/称呼。
 */
@Data
public class DineInOrderDTO implements Serializable {

    /**
     * 桌号或客户称呼（比如"A3桌"、"王先生"），方便后厨叫号
     */
    private String tableNo;

    /**
     * 客户联系电话（选填，方便后续联系）
     */
    private String phone;

    /**
     * 订单备注（比如"不要辣"、"孩子吃少放盐"）
     */
    private String remark;

    /**
     * 开单清单：客户点的菜品/套餐列表
     */
    private List<Item> items;

    /**
     * 清单里的每一项（一个菜品或一个套餐）
     */
    @Data
    public static class Item implements Serializable {
        /**
         * 菜品id（点的是菜品时有值）
         */
        private Integer dishId;
        /**
         * 套餐id（点的是套餐时有值）
         */
        private Integer setmealId;
        /**
         * 口味（比如"麻辣"、"微辣"，选填）
         */
        private String dishFlavor;
        /**
         * 点的份数
         */
        private Integer number;
    }
}
