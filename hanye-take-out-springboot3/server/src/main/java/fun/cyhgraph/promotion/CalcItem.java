package fun.cyhgraph.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠计算用的"已点菜品"模型
 * 小白讲解：营销引擎算账时不需要菜品图片、口味这些，只关心：
 * 这道菜属于哪个分类、id是多少、单价多少、点了几份，所以单独做一个精简对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcItem implements Serializable {

    /**
     * 菜品id（点的是菜品时有值）
     */
    private Integer dishId;
    /**
     * 套餐id（点的是套餐时有值）
     */
    private Integer setmealId;
    /**
     * 菜品所属分类id（判断"指定分类活动"时要用）
     */
    private Integer categoryId;
    /**
     * 菜品名称（生成优惠提示文案时要用）
     */
    private String name;
    /**
     * 数据库里的真实单价
     */
    private BigDecimal unitPrice;
    /**
     * 点的份数
     */
    private Integer number;
}
