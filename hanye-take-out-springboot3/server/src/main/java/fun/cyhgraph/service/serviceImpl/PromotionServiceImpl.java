package fun.cyhgraph.service.serviceImpl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.entity.Promotion;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.mapper.PromotionMapper;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现
 * 小白讲解：Controller 收到的表单里，满减档位是一个列表、折扣是一个小数、适用范围是id数组，
 * 这里负责把它们拼成数据库存的 ruleJson 字符串和 scopeIds 字符串；
 * 查出来时再反过来解析成列表，给前端表单回显。
 */
@Service
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionMapper promotionMapper;

    /**
     * 分页查询
     */
    @Override
    public PageResult pageQuery(int page, int pageSize, String name, Integer status, Integer type) {
        PageHelper.startPage(page, pageSize);
        Page<Promotion> pageData = promotionMapper.pageQuery(
                name == null || name.trim().isEmpty() ? null : name.trim(), status, type);
        // 列表里也要把规则解析出来（前端表格要直接展示"8.8折""满30减5"等摘要）
        for (Promotion promotion : pageData) {
            parseRule(promotion);
        }
        return new PageResult(pageData.getTotal(), pageData.getResult());
    }

    /**
     * 根据id查询并解析规则
     */
    @Override
    public Promotion getById(Integer id) {
        Promotion promotion = promotionMapper.getById(id);
        if (promotion != null) {
            parseRule(promotion);
        }
        return promotion;
    }

    /**
     * 新增
     */
    @Override
    public void save(Promotion promotion) {
        normalizeAndValidate(promotion);
        promotion.setStatus(promotion.getStatus() == null ? 1 : promotion.getStatus());
        promotionMapper.insert(promotion);
        log.info("新增营销活动：{}", promotion.getName());
    }

    /**
     * 修改
     */
    @Override
    public void update(Promotion promotion) {
        if (promotion.getId() == null || promotionMapper.getById(promotion.getId()) == null) {
            throw new OrderBusinessException("活动不存在");
        }
        normalizeAndValidate(promotion);
        promotionMapper.update(promotion);
        log.info("修改营销活动：{}", promotion.getName());
    }

    /**
     * 启停
     */
    @Override
    public void onOff(Integer id) {
        promotionMapper.onOff(id);
    }

    /**
     * 删除
     */
    @Override
    public void deleteById(Integer id) {
        promotionMapper.delete(id);
    }

    /**
     * 当前生效中的活动（开单页提示用）
     */
    @Override
    public List<Promotion> listActive() {
        List<Promotion> list = promotionMapper.listActive(LocalDateTime.now());
        for (Promotion promotion : list) {
            parseRule(promotion);
        }
        return list;
    }

    /**
     * 保存前统一处理：校验 + 把表单字段转成数据库存储格式
     */
    private void normalizeAndValidate(Promotion promotion) {
        if (promotion.getName() == null || promotion.getName().trim().isEmpty()) {
            throw new OrderBusinessException("请填写活动名称");
        }
        if (promotion.getType() == null) {
            throw new OrderBusinessException("请选择活动类型");
        }
        if (promotion.getBeginTime() == null || promotion.getEndTime() == null) {
            throw new OrderBusinessException("请选择活动起止时间");
        }
        if (!promotion.getEndTime().isAfter(promotion.getBeginTime())) {
            throw new OrderBusinessException("结束时间必须晚于开始时间");
        }
        if (promotion.getScopeType() == null) {
            promotion.setScopeType(Promotion.SCOPE_ALL);
        }

        // 适用范围id数组 -> "3,7,9" 字符串；非全场必须选了菜品/分类
        List<Integer> scopeIdList = promotion.getScopeIdList();
        if (Promotion.SCOPE_ALL.equals(promotion.getScopeType())) {
            promotion.setScopeIds(null);
        } else {
            if (scopeIdList == null || scopeIdList.isEmpty()) {
                throw new OrderBusinessException("请选择活动适用的分类或菜品");
            }
            promotion.setScopeIds(scopeIdList.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        // 按类型组装规则JSON并做相应校验
        Integer type = promotion.getType();
        if (Promotion.FULL_REDUCTION.equals(type)) {
            List<Promotion.Tier> tiers = promotion.getTiers();
            if (tiers == null || tiers.isEmpty()) {
                throw new OrderBusinessException("请至少添加一档满减规则");
            }
            List<Promotion.Tier> validTiers = new ArrayList<>();
            for (Promotion.Tier tier : tiers) {
                if (tier.getThreshold() == null || tier.getReduce() == null
                        || tier.getThreshold().compareTo(BigDecimal.ZERO) <= 0
                        || tier.getReduce().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new OrderBusinessException("满减门槛和减免金额都必须大于0");
                }
                if (tier.getReduce().compareTo(tier.getThreshold()) >= 0) {
                    throw new OrderBusinessException("减免金额不能大于或等于满减门槛");
                }
                validTiers.add(tier);
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("tiers", validTiers);
            promotion.setRuleJson(JSON.toJSONString(rule));
        } else if (Promotion.DISCOUNT.equals(type)) {
            BigDecimal discount = promotion.getDiscount();
            if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0
                    || discount.compareTo(BigDecimal.TEN) >= 0) {
                throw new OrderBusinessException("折扣值需在0.1~9.9之间，如8.8表示8.8折");
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("discount", discount);
            promotion.setRuleJson(JSON.toJSONString(rule));
        } else {
            // 第二份半价 / 买一送一 没有额外参数
            promotion.setRuleJson("{}");
        }
    }

    /**
     * 把数据库里的 ruleJson / scopeIds 解析回表单字段（tiers、discount、scopeIdList）
     */
    private void parseRule(Promotion promotion) {
        try {
            if (promotion.getRuleJson() != null && !promotion.getRuleJson().isEmpty()) {
                com.alibaba.fastjson.JSONObject rule = JSON.parseObject(promotion.getRuleJson());
                if (Promotion.FULL_REDUCTION.equals(promotion.getType()) && rule.containsKey("tiers")) {
                    promotion.setTiers(rule.getJSONArray("tiers").toJavaList(Promotion.Tier.class));
                }
                if (Promotion.DISCOUNT.equals(promotion.getType()) && rule.containsKey("discount")) {
                    promotion.setDiscount(rule.getBigDecimal("discount"));
                }
            }
        } catch (Exception e) {
            log.warn("活动[{}]规则JSON解析失败：{}", promotion.getId(), promotion.getRuleJson());
        }
        if (promotion.getScopeIds() != null && !promotion.getScopeIds().trim().isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (String s : promotion.getScopeIds().split(",")) {
                if (!s.trim().isEmpty()) {
                    ids.add(Integer.valueOf(s.trim()));
                }
            }
            promotion.setScopeIdList(ids);
        } else {
            promotion.setScopeIdList(new ArrayList<>());
        }
    }
}
