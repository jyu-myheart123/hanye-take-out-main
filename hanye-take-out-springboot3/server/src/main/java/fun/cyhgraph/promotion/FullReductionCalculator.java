package fun.cyhgraph.promotion;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.cyhgraph.entity.Promotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 满减计算器（如：满30减5、满60减12，多档只取顾客能达到的最高一档，不重复叠加）
 * 小白讲解：先看参加活动的菜一共多少钱，再从高档往低档找，达到哪档就减哪档的钱。
 */
@Component
public class FullReductionCalculator extends AbstractDiscountCalculator {

    @Override
    public Integer supportType() {
        return Promotion.FULL_REDUCTION;
    }

    @Override
    public PromotionResult calculate(Promotion promotion, List<CalcItem> items) {
        // 1. 只统计活动范围内的菜品金额
        BigDecimal scopeTotal = subtotal(scopeItems(promotion, items));

        // 2. 解析规则JSON：{"tiers":[{"threshold":30,"reduce":5},{"threshold":60,"reduce":12}]}
        JSONObject rule = JSON.parseObject(promotion.getRuleJson());
        if (rule == null) {
            return null;
        }
        JSONArray tiers = rule.getJSONArray("tiers");
        if (tiers == null || tiers.isEmpty()) {
            return null;
        }

        // 3. 从最高档往下找第一个满足的门槛（高档减得最多，对顾客最优惠）
        BigDecimal bestThreshold = null;
        BigDecimal bestReduce = null;
        for (int i = 0; i < tiers.size(); i++) {
            JSONObject tier = tiers.getJSONObject(i);
            BigDecimal threshold = tier.getBigDecimal("threshold");
            BigDecimal reduce = tier.getBigDecimal("reduce");
            if (threshold == null || reduce == null) {
                continue;
            }
            if (scopeTotal.compareTo(threshold) >= 0) {
                if (bestThreshold == null || threshold.compareTo(bestThreshold) > 0) {
                    bestThreshold = threshold;
                    bestReduce = reduce;
                }
            }
        }
        if (bestReduce == null || bestReduce.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // 一档都没达到
        }
        // 减免金额不能超过参与活动的商品总额（极端配置保护）
        if (bestReduce.compareTo(scopeTotal) > 0) {
            bestReduce = scopeTotal;
        }

        String tip = "满" + bestThreshold.stripTrailingZeros().toPlainString()
                + "减" + bestReduce.stripTrailingZeros().toPlainString()
                + "，本单减 ¥" + money(bestReduce).toPlainString();
        return result(promotion, bestReduce, Collections.singletonList(tip));
    }
}
