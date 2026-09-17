package fun.cyhgraph.promotion;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import fun.cyhgraph.entity.Promotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 折扣计算器（如：全场8.8折、指定分类9折）
 * 小白讲解：ruleJson 里 {"discount":8.8} 表示8.8折（付原价的88%），
 * 优惠金额 = 参加活动菜品的小计 × (10 - 8.8) / 10。
 */
@Component
public class RateDiscountCalculator extends AbstractDiscountCalculator {

    @Override
    public Integer supportType() {
        return Promotion.DISCOUNT;
    }

    @Override
    public PromotionResult calculate(Promotion promotion, List<CalcItem> items) {
        List<CalcItem> scope = scopeItems(promotion, items);
        if (scope.isEmpty()) {
            return null;
        }
        BigDecimal scopeTotal = subtotal(scope);

        // 解析折数，如 8.8
        JSONObject rule = JSON.parseObject(promotion.getRuleJson());
        if (rule == null) {
            return null;
        }
        BigDecimal discount = rule.getBigDecimal("discount");
        // 折数必须在 0.1~9.9 之间，不打折(>=10)或倒贴(<=0)的配置直接忽略
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0
                || discount.compareTo(BigDecimal.TEN) >= 0) {
            return null;
        }

        // 优惠 = 小计 × (10 - 折数) / 10
        BigDecimal saving = scopeTotal
                .multiply(BigDecimal.TEN.subtract(discount))
                .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
        if (saving.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        String scopeText;
        if (Promotion.SCOPE_CATEGORY.equals(promotion.getScopeType())) {
            scopeText = "指定分类";
        } else if (Promotion.SCOPE_DISH.equals(promotion.getScopeType())) {
            scopeText = "指定菜品";
        } else {
            scopeText = "全场";
        }
        String tip = scopeText + discount.stripTrailingZeros().toPlainString() + "折"
                + "，本单减 ¥" + money(saving).toPlainString();
        return result(promotion, saving, Collections.singletonList(tip));
    }
}
