package fun.cyhgraph.promotion;

import fun.cyhgraph.entity.Promotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 买一送一计算器
 * 小白讲解：同一道菜点2份，只收1份的钱（送的1份免单）；点3份收2份的钱。
 * 免单份数 = 份数 / 2 取整数部分，优惠 = 免单份数 × 单价。
 */
@Component
public class BuyOneGetOneCalculator extends AbstractDiscountCalculator {

    @Override
    public Integer supportType() {
        return Promotion.BUY_ONE_GET_ONE;
    }

    @Override
    public PromotionResult calculate(Promotion promotion, List<CalcItem> items) {
        BigDecimal totalSaving = BigDecimal.ZERO;
        List<String> tips = new ArrayList<>();

        for (CalcItem item : scopeItems(promotion, items)) {
            int number = item.getNumber() == null ? 0 : item.getNumber();
            int freeCount = number / 2; // 每2份免1份
            if (freeCount <= 0) {
                continue;
            }
            BigDecimal lineSaving = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(freeCount))
                    .setScale(2, RoundingMode.HALF_UP);
            totalSaving = totalSaving.add(lineSaving);
            tips.add(item.getName() + " 买一送一 x" + freeCount
                    + "，减 ¥" + lineSaving.toPlainString());
        }

        if (totalSaving.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return result(promotion, totalSaving, tips);
    }
}
