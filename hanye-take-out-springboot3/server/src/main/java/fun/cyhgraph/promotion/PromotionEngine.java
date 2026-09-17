package fun.cyhgraph.promotion;

import fun.cyhgraph.entity.Promotion;
import fun.cyhgraph.mapper.PromotionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 营销活动引擎
 * 小白讲解：开单算账时只找它——它负责把"当前时间生效的所有活动"挨个算一遍，
 * 自动选出"给顾客省得最多"的那一个。员工和顾客都不用自己算，也不怕算错。
 * 多个活动同时命中时的取舍规则：顾客最优惠（省钱最多者胜，省钱一样时取先配置的）。
 */
@Component
@Slf4j
public class PromotionEngine {

    @Autowired
    private PromotionMapper promotionMapper;

    /**
     * Spring 会把所有 DiscountCalculator 实现类（4个）注入成一个 List
     */
    @Autowired
    private List<DiscountCalculator> calculatorList;

    /**
     * 活动类型 -> 对应计算器，启动时建好，算账时直接取
     */
    private final Map<Integer, DiscountCalculator> calculatorMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (DiscountCalculator calculator : calculatorList) {
            calculatorMap.put(calculator.supportType(), calculator);
        }
        log.info("营销活动引擎初始化完成，已注册计算器：{}", calculatorMap.keySet());
    }

    /**
     * 计算当前清单能享受的最优活动
     *
     * @param items 已点菜品（精简模型）
     * @return 最优活动的优惠结果；没有任何可用活动时返回 null
     */
    public PromotionResult calculateBest(List<CalcItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        // 只看"启用中 + 当前时间在活动起止时间内"的活动
        List<Promotion> activeList = promotionMapper.listActive(LocalDateTime.now());
        if (activeList == null || activeList.isEmpty()) {
            return null;
        }

        PromotionResult best = null;
        for (Promotion promotion : activeList) {
            DiscountCalculator calculator = calculatorMap.get(promotion.getType());
            if (calculator == null) {
                continue;
            }
            try {
                PromotionResult result = calculator.calculate(promotion, items);
                if (result == null || result.getSaving() == null
                        || result.getSaving().signum() <= 0) {
                    continue;
                }
                // 省钱更多的胜出；省钱相同保持先配置的（活动按id升序查出来）
                if (best == null || result.getSaving().compareTo(best.getSaving()) > 0) {
                    best = result;
                }
            } catch (Exception e) {
                // 单个活动配置异常（比如JSON写错）不影响其他活动和正常下单
                log.error("活动[{}]计算异常，已跳过", promotion.getName(), e);
            }
        }
        if (best != null) {
            log.info("营销引擎计算结果：命中活动【{}】，优惠 ¥{}", best.getName(), best.getSaving());
        }
        return best;
    }
}
