package fun.cyhgraph.service;

import fun.cyhgraph.dto.MemberAddDTO;
import fun.cyhgraph.dto.MemberPageDTO;
import fun.cyhgraph.dto.MemberPointsDTO;
import fun.cyhgraph.dto.MemberRechargeDTO;
import fun.cyhgraph.entity.Member;
import fun.cyhgraph.entity.MemberLevelRule;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.vo.MemberConsumeResult;
import fun.cyhgraph.vo.MemberVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员服务接口
 */
public interface MemberService {

    /**
     * 会员分页查询（姓名/手机号模糊、等级筛选）
     */
    PageResult pageQuery(MemberPageDTO memberPageDTO);

    /**
     * 根据id查会员详情（带等级名称和折扣）
     */
    MemberVO getById(Integer id);

    /**
     * 按手机号识别会员：手机号为空或查无此人返回null，会员被禁用则抛异常
     */
    MemberVO recognizeByPhone(String phone);

    /**
     * 手动新增会员（办卡）
     */
    void add(MemberAddDTO memberAddDTO);

    /**
     * 会员充值（支持赠送，如充200送20）
     */
    void recharge(MemberRechargeDTO memberRechargeDTO);

    /**
     * 人工调整积分（正加负减）
     */
    void adjustPoints(MemberPointsDTO memberPointsDTO);

    /**
     * 查询某会员的流水（消费记录/充值记录），可按类型筛选
     */
    PageResult flows(Integer memberId, Integer type, int page, int pageSize);

    /**
     * 查询全部等级规则
     */
    List<MemberLevelRule> listRules();

    /**
     * 修改等级规则（门槛、折扣）
     */
    void updateRules(List<MemberLevelRule> rules);

    /**
     * 下单时按手机号查会员，查不到就自动建档（消费即注册）
     */
    Member registerIfAbsent(String phone);

    /**
     * 计算会员等级折扣能省多少钱（在活动优惠后的金额上再打折）
     */
    BigDecimal calcLevelDiscount(Member member, BigDecimal baseAfterPromotion);

    /**
     * 计算积分可抵扣金额（100积分抵5元，不能超过本单还需支付的金额）
     */
    BigDecimal calcPointsDeduction(Member member, BigDecimal capAmount, boolean usePoints);

    /**
     * 下单后的会员结算：扣余额（乐观锁）、扣/增积分、累计消费、自动升级、写流水。
     * 全部和下单在同一个数据库事务里，任何一步失败整单回滚。
     */
    MemberConsumeResult consume(Integer orderId, String phone, BigDecimal payAmount,
                                BigDecimal pointsDeductionAmount, boolean payByBalance);
}
