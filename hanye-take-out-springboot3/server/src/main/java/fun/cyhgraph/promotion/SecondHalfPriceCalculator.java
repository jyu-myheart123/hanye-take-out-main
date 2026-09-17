package fun.cyhgraph.promotion;

import fun.cyhgraph.entity.Promotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 第二份半价计算器
 * 小白讲解：同一道菜点2份，第2份半价；点4份就有2份半价。
 * 按每道菜分别算：半价份数 = 份数 / 2 取整数部分，优惠 = 半价份数 × 单价 × 0.5。
 */
@Component
public class SecondHalfPriceCalculator extends AbstractDiscountCalculator {

    @Override
    public Integer supportType() {
        return Promotion.SECOND_HALF;
    }

    @Override
    public PromotionResult calculate(Promotion promotion, List<CalcItem> items) {
        BigDecimal totalSaving = BigDecimal.ZERO;
        List<String> tips = new ArrayList<>();

        for (CalcItem item : scopeItems(promotion, items)) {
            int number = item.getNumber() == null ? 0 : item.getNumber();
            int halfCount = number / 2; // 每2份里有1份半价（整数除法自动取整）
            if (halfCount <= 0) {
                continue;
            }
            BigDecimal lineSaving = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(halfCount))
                    .multiply(new BigDecimal("0.5"))
                    .setScale(2, RoundingMode.HALF_UP);
            totalSaving = totalSaving.add(lineSaving);
            tips.add(item.getName() + " 第二份半价 x" + halfCount
                    + "，减 ¥" + lineSaving.toPlainString());
        }

        if (totalSaving.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // 范围内的菜都只点了1份，享受不到
        }
        return result(promotion, totalSaving, tips);
    }
}
