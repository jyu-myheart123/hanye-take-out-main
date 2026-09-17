package fun.cyhgraph.service.serviceImpl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.dto.MemberAddDTO;
import fun.cyhgraph.dto.MemberPageDTO;
import fun.cyhgraph.dto.MemberPointsDTO;
import fun.cyhgraph.dto.MemberRechargeDTO;
import fun.cyhgraph.entity.Member;
import fun.cyhgraph.entity.MemberFlow;
import fun.cyhgraph.entity.MemberLevelRule;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.mapper.MemberFlowMapper;
import fun.cyhgraph.mapper.MemberLevelRuleMapper;
import fun.cyhgraph.mapper.MemberMapper;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.MemberService;
import fun.cyhgraph.vo.MemberConsumeResult;
import fun.cyhgraph.vo.MemberVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 会员服务实现
 * 小白讲解：会员的建档、识别、充值、积分、等级折扣、消费结算都在这里。
 * 记住两个关键规则：1元=1积分；100积分=5元。等级靠累计消费自动升级。
 */
@Service
@Slf4j
public class MemberServiceImpl implements MemberService {

    /**
     * 手机号校验：1开头的11位数字
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    /**
     * 100积分抵5元，即1积分=0.05元
     */
    private static final BigDecimal POINT_RATE = new BigDecimal("0.05");

    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private MemberFlowMapper memberFlowMapper;
    @Autowired
    private MemberLevelRuleMapper memberLevelRuleMapper;

    /**
     * 项目启动时自动补齐默认等级规则（即使忘了执行SQL脚本也能用）
     */
    @PostConstruct
    public void initDefaultRules() {
        if (memberLevelRuleMapper.count() == 0) {
            memberLevelRuleMapper.insert(MemberLevelRule.builder()
                    .level(1).levelName("银卡会员").thresholdAmount(BigDecimal.ZERO).discount(new BigDecimal("1.00")).build());
            memberLevelRuleMapper.insert(MemberLevelRule.builder()
                    .level(2).levelName("金卡会员").thresholdAmount(new BigDecimal("500")).discount(new BigDecimal("0.95")).build());
            memberLevelRuleMapper.insert(MemberLevelRule.builder()
                    .level(3).levelName("钻石会员").thresholdAmount(new BigDecimal("2000")).discount(new BigDecimal("0.90")).build());
            log.info("会员等级规则为空，已自动初始化默认规则（银卡/金卡/钻石）");
        }
    }

    /**
     * 分页查询
     */
    @Override
    public PageResult pageQuery(MemberPageDTO memberPageDTO) {
        PageHelper.startPage(memberPageDTO.getPage(), memberPageDTO.getPageSize());
        Page<MemberVO> pageData = memberMapper.pageQuery(memberPageDTO);
        return new PageResult(pageData.getTotal(), pageData.getResult());
    }

    /**
     * 根据id查会员（带等级名称和折扣）
     */
    @Override
    public MemberVO getById(Integer id) {
        Member member = memberMapper.getById(id);
        return member == null ? null : toVO(member);
    }

    /**
     * 手机号识别会员
     */
    @Override
    public MemberVO recognizeByPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            return null;
        }
        Member member = memberMapper.getByPhone(normalized);
        if (member == null) {
            return null;
        }
        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new OrderBusinessException("该会员账号已被禁用，请联系管理员");
        }
        return toVO(member);
    }

    /**
     * 手动新增会员
     */
    @Override
    public void add(MemberAddDTO memberAddDTO) {
        String phone = normalizePhone(memberAddDTO.getPhone());
        if (phone == null) {
            throw new OrderBusinessException("请输入正确的11位手机号");
        }
        if (memberMapper.getByPhone(phone) != null) {
            throw new OrderBusinessException("该手机号已是会员，无需重复建档");
        }
        Member member = Member.builder()
                .name(memberAddDTO.getName())
                .phone(phone)
                .balance(BigDecimal.ZERO)
                .points(0)
                .totalRecharge(BigDecimal.ZERO)
                .totalGift(BigDecimal.ZERO)
                .totalConsume(BigDecimal.ZERO)
                .level(Member.LEVEL_SILVER)
                .status(1)
                .build();
        memberMapper.insert(member);
        log.info("手动新增会员：{} {}", phone, memberAddDTO.getName());
    }

    /**
     * 充值（本金+赠送一起到账）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(MemberRechargeDTO dto) {
        Member member = memberMapper.getById(dto.getMemberId());
        if (member == null) {
            throw new OrderBusinessException("会员不存在");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("充值本金必须大于0");
        }
        BigDecimal gift = dto.getGift() == null ? BigDecimal.ZERO : dto.getGift();
        if (gift.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderBusinessException("赠送金额不能为负");
        }
        BigDecimal total = dto.getAmount().add(gift);
        memberMapper.recharge(member.getId(), dto.getAmount(), gift, total);

        // 记充值流水，记录充值后的余额
        Member fresh = memberMapper.getById(member.getId());
        MemberFlow flow = MemberFlow.builder()
                .memberId(member.getId())
                .type(MemberFlow.TYPE_RECHARGE)
                .amount(total)
                .points(0)
                .balanceAfter(fresh.getBalance())
                .pointsAfter(fresh.getPoints())
                .remark(gift.compareTo(BigDecimal.ZERO) > 0
                        ? "充值¥" + dto.getAmount().stripTrailingZeros().toPlainString()
                        + "，赠送¥" + gift.stripTrailingZeros().toPlainString()
                        : "充值¥" + dto.getAmount().stripTrailingZeros().toPlainString())
                .build();
        memberFlowMapper.insert(flow);
        log.info("会员[{}]充值 ¥{}（赠送¥{}）", member.getPhone(), dto.getAmount(), gift);
    }

    /**
     * 人工调整积分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(MemberPointsDTO dto) {
        Member member = memberMapper.getById(dto.getMemberId());
        if (member == null) {
            throw new OrderBusinessException("会员不存在");
        }
        if (dto.getPoints() == null || dto.getPoints() == 0) {
            throw new OrderBusinessException("调整积分不能为0");
        }
        int rows = memberMapper.addPoints(member.getId(), dto.getPoints());
        if (rows == 0) {
            throw new OrderBusinessException("会员现有积分不足，无法扣减" + Math.abs(dto.getPoints()) + "分");
        }
        Member fresh = memberMapper.getById(member.getId());
        MemberFlow flow = MemberFlow.builder()
                .memberId(member.getId())
                .type(MemberFlow.TYPE_MANUAL_ADJUST)
                .amount(BigDecimal.ZERO)
                .points(dto.getPoints())
                .balanceAfter(fresh.getBalance())
                .pointsAfter(fresh.getPoints())
                .remark(dto.getRemark() == null || dto.getRemark().isEmpty()
                        ? "人工调整积分" : dto.getRemark())
                .build();
        memberFlowMapper.insert(flow);
    }

    /**
     * 会员流水分页
     */
    @Override
    public PageResult flows(Integer memberId, Integer type, int page, int pageSize) {
        PageHelper.startPage(page, pageSize);
        Page<MemberFlow> pageData = memberFlowMapper.pageByMember(memberId, type);
        return new PageResult(pageData.getTotal(), pageData.getResult());
    }

    /**
     * 全部等级规则
     */
    @Override
    public List<MemberLevelRule> listRules() {
        return memberLevelRuleMapper.listAll();
    }

    /**
     * 修改等级规则（改完后立刻按新规则重新判定所有会员等级由消费时触发，这里只更新配置）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRules(List<MemberLevelRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new OrderBusinessException("规则不能为空");
        }
        for (MemberLevelRule rule : rules) {
            if (rule.getThresholdAmount() == null || rule.getThresholdAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new OrderBusinessException("升级门槛不能为负");
            }
            if (rule.getDiscount() == null || rule.getDiscount().compareTo(BigDecimal.ZERO) <= 0
                    || rule.getDiscount().compareTo(BigDecimal.ONE) > 0) {
                throw new OrderBusinessException("折扣需在0~1之间，如0.95表示95折");
            }
            memberLevelRuleMapper.update(rule.getLevel(), rule.getLevelName(),
                    rule.getThresholdAmount(), rule.getDiscount());
        }
    }

    /**
     * 下单时查不到会员就自动建档（银卡起步），消费即注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member registerIfAbsent(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            throw new OrderBusinessException("请输入正确的11位会员手机号");
        }
        Member member = memberMapper.getByPhone(normalized);
        if (member != null) {
            if (member.getStatus() != null && member.getStatus() == 0) {
                throw new OrderBusinessException("该会员账号已被禁用，无法下单");
            }
            return member;
        }
        member = Member.builder()
                .name(null)
                .phone(normalized)
                .balance(BigDecimal.ZERO)
                .points(0)
                .totalRecharge(BigDecimal.ZERO)
                .totalGift(BigDecimal.ZERO)
                .totalConsume(BigDecimal.ZERO)
                .level(Member.LEVEL_SILVER)
                .status(1)
                .build();
        memberMapper.insert(member);
        log.info("会员首次消费自动建档：{}", normalized);
        return member;
    }

    /**
     * 会员等级折扣：在活动优惠后的金额上按等级折扣率再打折，返回能省多少钱
     */
    @Override
    public BigDecimal calcLevelDiscount(Member member, BigDecimal baseAfterPromotion) {
        if (member == null || baseAfterPromotion == null
                || baseAfterPromotion.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        MemberLevelRule rule = memberLevelRuleMapper.getByLevel(member.getLevel());
        if (rule == null || rule.getDiscount() == null
                || rule.getDiscount().compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.ZERO; // 银卡默认不打折
        }
        // 优惠金额 = 基数 × (1 - 折扣率)，如 0.95 → 省5%
        BigDecimal saving = baseAfterPromotion.multiply(BigDecimal.ONE.subtract(rule.getDiscount()))
                .setScale(2, RoundingMode.HALF_UP);
        // 保护：折扣金额不能超过基数
        return saving.min(baseAfterPromotion);
    }

    /**
     * 积分抵扣：100分抵5元，可抵金额不能超过本单待支付金额
     */
    @Override
    public BigDecimal calcPointsDeduction(Member member, BigDecimal capAmount, boolean usePoints) {
        if (!usePoints || member == null || member.getPoints() == null || member.getPoints() < 100) {
            return BigDecimal.ZERO;
        }
        if (capAmount == null || capAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // 100分一个兑换单位，每单位5元
        int units = member.getPoints() / 100;
        BigDecimal deduction = new BigDecimal(units * 5);
        // 最多抵到本单金额，不能抵出负数
        return deduction.min(capAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 下单后的会员结算（和下单在同一个事务中执行）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberConsumeResult consume(Integer orderId, String phone, BigDecimal payAmount,
                                       BigDecimal pointsDeductionAmount, boolean payByBalance) {
        // 1. 识别会员或自动建档
        Member member = registerIfAbsent(phone);

        // 2. 余额支付：乐观锁扣减，余额不足时SQL影响0行，直接抛异常回滚整单
        if (payByBalance && payAmount.compareTo(BigDecimal.ZERO) > 0) {
            int rows = memberMapper.deductBalance(member.getId(), payAmount);
            if (rows == 0) {
                throw new OrderBusinessException("会员余额不足，无法使用余额支付");
            }
        }

        // 3. 积分换算：用掉多少分（0.05元/分）、本单返多少分（实付1元=1分，向下取整）
        int usedPoints = pointsDeductionAmount.multiply(new BigDecimal("20"))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        int earnedPoints = payAmount.setScale(0, RoundingMode.DOWN).intValue();
        int pointDelta = earnedPoints - usedPoints;
        if (pointDelta != 0) {
            int rows = memberMapper.addPoints(member.getId(), pointDelta);
            if (rows == 0) {
                throw new OrderBusinessException("会员积分不足，无法完成积分抵扣");
            }
        }

        // 4. 累计消费，按等级规则自动升级
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            memberMapper.addConsume(member.getId(), payAmount);
        }

        // 5. 重新读会员最新数据，判定等级
        Member fresh = memberMapper.getById(member.getId());
        Integer targetLevel = resolveLevel(fresh.getTotalConsume());
        if (!targetLevel.equals(fresh.getLevel())) {
            memberMapper.updateLevel(fresh.getId(), targetLevel);
            fresh.setLevel(targetLevel);
            log.info("会员[{}]累计消费达到门槛，升级为 level={}", fresh.getPhone(), targetLevel);
        }
        MemberLevelRule levelRule = memberLevelRuleMapper.getByLevel(fresh.getLevel());

        // 6. 写流水（余额消费、积分抵扣、消费返积分分开记，会员页消费记录看得明白）
        if (payByBalance && payAmount.compareTo(BigDecimal.ZERO) > 0) {
            memberFlowMapper.insert(MemberFlow.builder()
                    .memberId(fresh.getId())
                    .type(MemberFlow.TYPE_CONSUME)
                    .amount(payAmount.negate())
                    .points(0)
                    .balanceAfter(fresh.getBalance())
                    .pointsAfter(fresh.getPoints())
                    .orderId(orderId)
                    .remark("堂食消费 ¥" + payAmount.toPlainString())
                    .build());
        }
        if (usedPoints > 0) {
            memberFlowMapper.insert(MemberFlow.builder()
                    .memberId(fresh.getId())
                    .type(MemberFlow.TYPE_POINTS_DEDUCT)
                    .amount(BigDecimal.ZERO)
                    .points(-usedPoints)
                    .balanceAfter(fresh.getBalance())
                    .pointsAfter(fresh.getPoints())
                    .orderId(orderId)
                    .remark("积分抵扣 ¥" + pointsDeductionAmount.toPlainString())
                    .build());
        }
        if (earnedPoints > 0) {
            memberFlowMapper.insert(MemberFlow.builder()
                    .memberId(fresh.getId())
                    .type(MemberFlow.TYPE_POINTS_EARN)
                    .amount(BigDecimal.ZERO)
                    .points(earnedPoints)
                    .balanceAfter(fresh.getBalance())
                    .pointsAfter(fresh.getPoints())
                    .orderId(orderId)
                    .remark("堂食消费返积分")
                    .build());
        }

        return MemberConsumeResult.builder()
                .memberId(fresh.getId())
                .level(fresh.getLevel())
                .levelName(levelRule == null ? null : levelRule.getLevelName())
                .balanceAfter(fresh.getBalance())
                .pointsAfter(fresh.getPoints())
                .pointsEarned(earnedPoints)
                .pointsUsed(usedPoints)
                .build();
    }

    /**
     * 根据累计消费判定应属等级：从高到低匹配第一个达到门槛的等级
     */
    private Integer resolveLevel(BigDecimal totalConsume) {
        List<MemberLevelRule> rules = memberLevelRuleMapper.listAll(); // 已按level升序
        Integer level = Member.LEVEL_SILVER;
        for (MemberLevelRule rule : rules) {
            if (totalConsume.compareTo(rule.getThresholdAmount()) >= 0
                    && rule.getLevel() > level) {
                level = rule.getLevel();
            }
        }
        return level;
    }

    /**
     * 手机号去空格、校验格式，不合法返回null
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return PHONE_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    /**
     * Member 实体转 VO（附带等级名称和折扣）
     */
    private MemberVO toVO(Member member) {
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(member, vo);
        MemberLevelRule rule = memberLevelRuleMapper.getByLevel(member.getLevel());
        if (rule != null) {
            vo.setLevelName(rule.getLevelName());
            vo.setDiscount(rule.getDiscount());
        }
        return vo;
    }
}
