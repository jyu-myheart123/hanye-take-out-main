package fun.cyhgraph.promotion;

import fun.cyhgraph.entity.Promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠计算器公共父类
 * 小白讲解：4种计算器都要用到的"判断菜品在不在活动范围里""算小计""四舍五入"
 * 这些重复操作，统一抽到父类里，子类直接复用。
 */
public abstract class AbstractDiscountCalculator implements DiscountCalculator {

    /**
     * 金额统一保留2位小数，四舍五入（人民币最小到分）
     */
    protected BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 解析活动适用范围id（数据库里存的是 "3,7,9" 这种逗号拼接字符串）
     */
    protected List<Integer> scopeIds(Promotion promotion) {
        if (promotion.getScopeIds() == null || promotion.getScopeIds().trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(promotion.getScopeIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 判断一道菜是否在活动适用范围内
     * 全场=都参加；指定分类=分类id命中即可；指定菜品=只有对应菜品id才算（套餐不参加菜品级活动）
     */
    protected boolean inScope(Promotion promotion, CalcItem item, List<Integer> idList) {
        if (promotion.getScopeType() == null || Promotion.SCOPE_ALL.equals(promotion.getScopeType())) {
            return true;
        }
        if (Promotion.SCOPE_CATEGORY.equals(promotion.getScopeType())) {
            return item.getCategoryId() != null && idList.contains(item.getCategoryId());
        }
        if (Promotion.SCOPE_DISH.equals(promotion.getScopeType())) {
            return item.getDishId() != null && idList.contains(item.getDishId());
        }
        return false;
    }

    /**
     * 过滤出参加本活动的菜品
     */
    protected List<CalcItem> scopeItems(Promotion promotion, List<CalcItem> items) {
        List<Integer> idList = scopeIds(promotion);
        return items.stream()
                .filter(item -> inScope(promotion, item, idList))
                .collect(Collectors.toList());
    }

    /**
     * 计算范围内菜品的原价小计
     */
    protected BigDecimal subtotal(List<CalcItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CalcItem item : items) {
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getNumber())));
        }
        return total;
    }

    /**
     * 快速构造一个计算结果
     */
    protected PromotionResult result(Promotion promotion, BigDecimal saving, List<String> tips) {
        return PromotionResult.builder()
                .promotionId(promotion.getId())
                .name(promotion.getName())
                .type(promotion.getType())
                .saving(money(saving))
                .tips(tips)
                .build();
    }
}
