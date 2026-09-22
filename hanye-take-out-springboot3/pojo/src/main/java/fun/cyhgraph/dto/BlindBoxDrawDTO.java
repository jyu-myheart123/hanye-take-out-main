package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 盲盒抽卡 DTO（顾客已付款，员工/顾客点下某张卡牌时提交）
 * 小白讲解：告诉后端"抽哪个盲盒、这是堂食还是外卖、桌号或收货信息"，
 * 后端随机选中一个套餐并生成正式订单，之后接单/做菜/配送全部走老流程。
 */
@Data
public class BlindBoxDrawDTO implements Serializable {

    /**
     * 盲盒id
     */
    private Integer boxId;
    /**
     * 顾客点了第几张卡（0/1/2）：抽中的套餐会被后端放到这个位置，翻的那张就是命运套餐
     */
    private Integer pickedIndex;
    /**
     * 订单类型：2堂食（默认） 1外卖
     */
    private Integer orderType;
    /**
     * 桌号/顾客称呼（堂食必填，后厨叫号用）
     */
    private String tableNo;
    /**
     * 联系电话（选填）
     */
    private String phone;
    /**
     * 顾客称呼（选填）
     */
    private String customerName;
    /**
     * 外卖收货地址（外卖时填写）
     */
    private String address;
    /**
     * 备注（选填）
     */
    private String remark;
}
