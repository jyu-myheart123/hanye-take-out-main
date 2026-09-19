package fun.cyhgraph.service.serviceImpl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.context.BaseContext;
import fun.cyhgraph.dto.StaffRatingDTO;
import fun.cyhgraph.dto.StaffRewardDTO;
import fun.cyhgraph.dto.StaffTipDTO;
import fun.cyhgraph.entity.Employee;
import fun.cyhgraph.entity.StaffRating;
import fun.cyhgraph.entity.StaffReward;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.mapper.EmployeeMapper;
import fun.cyhgraph.mapper.StaffRatingMapper;
import fun.cyhgraph.mapper.StaffRewardMapper;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.StaffService;
import fun.cyhgraph.vo.StaffRankVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 员工赏罚服务实现
 * 小白讲解：北极星积分规则很简单——服务态度被打几星，员工就得几个北极星积分；
 * 顾客给的小费和店主发的奖金都会写一条流水；排行榜按平均分排名，店主照着榜发周奖金。
 */
@Service
@Slf4j
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffRatingMapper staffRatingMapper;
    @Autowired
    private StaffRewardMapper staffRewardMapper;
    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 在职员工列表
     */
    @Override
    public List<Employee> listActiveEmployees() {
        return staffRatingMapper.listActiveEmployees();
    }

    /**
     * 提交员工服务评分
     * 小白讲解：先校验员工和星级是否合法；北极星积分=服务态度星数；保存后把评分id还给前端。
     */
    @Override
    public Integer submitRating(StaffRatingDTO dto) {
        if (dto.getEmployeeId() == null) {
            throw new OrderBusinessException("请选择被评价的服务员工");
        }
        Employee employee = employeeMapper.getById(dto.getEmployeeId());
        if (employee == null) {
            throw new OrderBusinessException("所选员工不存在");
        }
        Integer serviceScore = dto.getServiceScore();
        if (serviceScore == null || serviceScore < 1 || serviceScore > 5) {
            throw new OrderBusinessException("服务态度评分必须是1-5星");
        }
        Integer recommendScore = dto.getRecommendScore();
        if (recommendScore != null && (recommendScore < 1 || recommendScore > 5)) {
            throw new OrderBusinessException("推荐指数必须是1-5星");
        }

        // 组装评分记录：小费默认0（等弹窗后再补），状态默认有效
        StaffRating rating = StaffRating.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .orderId(dto.getOrderId())
                .tableNo(trimToNull(dto.getTableNo()))
                .customerName(trimToNull(dto.getCustomerName()))
                .serviceScore(serviceScore)
                .recommendScore(recommendScore)
                .content(trimToNull(dto.getContent()))
                .tipAmount(BigDecimal.ZERO)
                .starPoints(serviceScore)
                .status(1)
                .build();
        staffRatingMapper.insert(rating);
        log.info("员工服务评分已提交：员工={}，服务{}星，推荐{}星，评分id={}",
                employee.getName(), serviceScore, recommendScore, rating.getId());
        return rating.getId();
    }

    /**
     * 顾客打赏小费（评分之后弹窗确认才会调用）
     * 小白讲解：给小费要做两件事——把金额补到评分记录上，再写一条"顾客打赏"流水；
     * 用事务保证两件事要么一起成功要么一起失败。
     */
    @Override
    @Transactional
    public void tip(StaffTipDTO dto) {
        if (dto.getRatingId() == null) {
            throw new OrderBusinessException("缺少评分记录，无法打赏");
        }
        StaffRating rating = staffRatingMapper.getById(dto.getRatingId());
        if (rating == null) {
            throw new OrderBusinessException("评分记录不存在，无法打赏");
        }
        if (rating.getTipAmount() != null && rating.getTipAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new OrderBusinessException("这条评分已经打赏过了，不能重复打赏");
        }
        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("打赏金额必须大于0");
        }

        // 第一步：小费金额补到评分记录
        staffRatingMapper.updateTip(rating.getId(), amount);
        // 第二步：写打赏流水（type=1，打赏不另外加北极星积分，积分来自星级）
        StaffReward reward = StaffReward.builder()
                .employeeId(rating.getEmployeeId())
                .type(1)
                .amount(amount)
                .starPoints(0)
                .ratingId(rating.getId())
                .orderId(rating.getOrderId())
                .reason("顾客小费打赏")
                .operatorId(BaseContext.getCurrentId())
                .build();
        staffRewardMapper.insert(reward);
        log.info("顾客打赏：员工={}，金额={}，评分id={}", rating.getEmployeeName(), amount, rating.getId());
    }

    /**
     * 星光排行榜
     * 小白讲解：把前端选的时间范围翻译成一个"起始时间"，统计这个时间之后的有效评分。
     * 今日=今天0点开始；近3日/近7日=往前推3/7×24小时；近一月=往前推1个月。
     */
    @Override
    public List<StaffRankVO> rank(String range) {
        LocalDateTime beginTime;
        String r = range == null ? "" : range.trim().toLowerCase();
        switch (r) {
            case "3d":
                beginTime = LocalDateTime.now().minusDays(3);
                break;
            case "7d":
                beginTime = LocalDateTime.now().minusDays(7);
                break;
            case "month":
                beginTime = LocalDateTime.now().minusMonths(1);
                break;
            case "today":
            default:
                // 默认今日：从今天0点0分开始算
                beginTime = LocalDate.now().atStartOfDay();
                break;
        }
        List<StaffRankVO> list = staffRewardMapper.rankList(beginTime);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 评分记录分页
     */
    @Override
    public PageResult ratingPage(int page, int pageSize, Integer employeeId, Integer status, String keyword) {
        PageHelper.startPage(page, pageSize);
        String kw = trimToNull(keyword);
        Page<StaffRating> pageResult = staffRatingMapper.pageQuery(employeeId, status, kw);
        return new PageResult(pageResult.getTotal(), pageResult.getResult());
    }

    /**
     * 评分显示/隐藏
     */
    @Override
    public void ratingOnOff(Integer id) {
        if (staffRatingMapper.getById(id) == null) {
            throw new OrderBusinessException("评分记录不存在");
        }
        staffRatingMapper.onOff(id);
    }

    /**
     * 删除评分（只删评分本身，历史打赏流水保留留痕）
     */
    @Override
    public void ratingDelete(Integer id) {
        if (staffRatingMapper.getById(id) == null) {
            throw new OrderBusinessException("评分记录不存在");
        }
        staffRatingMapper.delete(id);
    }

    /**
     * 赏罚流水分页
     */
    @Override
    public PageResult rewardPage(int page, int pageSize, Integer employeeId, Integer type) {
        PageHelper.startPage(page, pageSize);
        Page<fun.cyhgraph.vo.StaffRewardVO> pageResult = staffRewardMapper.pageQuery(employeeId, type);
        return new PageResult(pageResult.getTotal(), pageResult.getResult());
    }

    /**
     * 店主奖励/惩戒（只有管理员 role=1 能操作）
     * 小白讲解：前端金额都传正数，这里根据类型把惩戒的钱和积分转成负数，流水里一眼就能区分。
     */
    @Override
    @Transactional
    public void addReward(StaffRewardDTO dto) {
        // 权限校验：从当前登录人信息判断是不是管理员
        Integer currentId = BaseContext.getCurrentId();
        Employee operator = currentId == null ? null : employeeMapper.getById(currentId);
        if (operator == null || operator.getRole() == null || operator.getRole() != 1) {
            throw new OrderBusinessException("只有管理员才能进行奖励或惩戒操作");
        }
        if (dto.getEmployeeId() == null) {
            throw new OrderBusinessException("请选择员工");
        }
        Employee employee = employeeMapper.getById(dto.getEmployeeId());
        if (employee == null) {
            throw new OrderBusinessException("所选员工不存在");
        }
        if (dto.getType() == null || (dto.getType() != 2 && dto.getType() != 3)) {
            throw new OrderBusinessException("赏罚类型不正确");
        }
        BigDecimal amount = dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderBusinessException("金额请填写正数，系统会按奖励/惩戒自动记正负");
        }
        int points = dto.getStarPoints() == null ? 0 : Math.abs(dto.getStarPoints());
        if (amount.compareTo(BigDecimal.ZERO) == 0 && points == 0) {
            throw new OrderBusinessException("奖励/惩戒的金额和积分不能都为0");
        }
        String reason = trimToNull(dto.getReason());
        if (reason == null) {
            throw new OrderBusinessException("请填写奖惩原因，方便后续核对");
        }

        boolean isPunish = dto.getType() == 3;
        StaffReward reward = StaffReward.builder()
                .employeeId(employee.getId())
                .type(dto.getType())
                // 惩戒：金额、积分都取反；奖励：保持正数
                .amount(isPunish ? amount.negate() : amount)
                .starPoints(isPunish ? -points : points)
                .reason(reason)
                .operatorId(currentId)
                .build();
        staffRewardMapper.insert(reward);
        log.info("店主{}：员工={}，金额={}，积分={}，原因={}",
                isPunish ? "惩戒" : "奖励", employee.getName(),
                reward.getAmount(), reward.getStarPoints(), reason);
    }

    /**
     * 小工具：把前端传的空字符串/全空格转成 null，避免数据库存一堆空白
     */
    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
