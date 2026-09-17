package fun.cyhgraph.promotion;

import fun.cyhgraph.entity.Promotion;

import java.util.List;

/**
 * 优惠计算器策略接口
 * 小白讲解：营销活动有4种（满减/折扣/第二份半价/买一送一），每种活动写一个实现类，
 * 都实现同一个接口，引擎不用关心具体算法，拿到活动按类型找对应计算器即可。
 * 以后新增"首单立减"只要再加一个实现类，老代码一行都不用改（策略模式）。
 */
public interface DiscountCalculator {

    /**
     * 该计算器支持的活动类型（对应 Promotion 里的常量）
     */
    Integer supportType();

    /**
     * 计算某个活动在当前清单上能优惠多少钱
     *
     * @param promotion 活动配置
     * @param items     已点菜品清单
     * @return 优惠结果；不满足活动条件（省0元）时返回 null
     */
    PromotionResult calculate(Promotion promotion, List<CalcItem> items);
}
