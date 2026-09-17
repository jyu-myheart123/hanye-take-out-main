package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.MemberAddDTO;
import fun.cyhgraph.dto.MemberPageDTO;
import fun.cyhgraph.dto.MemberPointsDTO;
import fun.cyhgraph.dto.MemberRechargeDTO;
import fun.cyhgraph.entity.MemberLevelRule;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.MemberService;
import fun.cyhgraph.vo.MemberVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 堂食会员管理接口（菜单：营销活动下面的"堂食会员"）
 * 小白讲解：员工在这个页面查看会员、办卡、充值（充200送20）、调整积分、
 * 查看消费流水、配置银卡/金卡/钻石的升级门槛和折扣。
 */
@RestController
@RequestMapping("/admin/member")
@Slf4j
public class MemberController {

    @Autowired
    private MemberService memberService;

    /**
     * 会员分页查询（姓名/手机号模糊、等级筛选）
     */
    @GetMapping("/page")
    public Result<PageResult> page(MemberPageDTO memberPageDTO) {
        log.info("会员分页查询：{}", memberPageDTO);
        return Result.success(memberService.pageQuery(memberPageDTO));
    }

    /**
     * 根据id查会员详情
     */
    @GetMapping("/{id}")
    public Result<MemberVO> getById(@PathVariable Integer id) {
        return Result.success(memberService.getById(id));
    }

    /**
     * 根据手机号识别会员（开单页/查会员都用它）
     */
    @GetMapping("/phone/{phone}")
    public Result<MemberVO> getByPhone(@PathVariable String phone) {
        return Result.success(memberService.recognizeByPhone(phone));
    }

    /**
     * 手动新增会员（办卡）
     */
    @PostMapping
    public Result add(@RequestBody MemberAddDTO memberAddDTO) {
        log.info("新增会员：{}", memberAddDTO.getPhone());
        memberService.add(memberAddDTO);
        return Result.success();
    }

    /**
     * 会员充值（支持赠送）
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestBody MemberRechargeDTO memberRechargeDTO) {
        log.info("会员充值：{}", memberRechargeDTO);
        memberService.recharge(memberRechargeDTO);
        return Result.success();
    }

    /**
     * 人工调整积分（正加负减）
     */
    @PutMapping("/points")
    public Result adjustPoints(@RequestBody MemberPointsDTO memberPointsDTO) {
        log.info("调整会员积分：{}", memberPointsDTO);
        memberService.adjustPoints(memberPointsDTO);
        return Result.success();
    }

    /**
     * 查询会员流水（充值/消费/积分记录）
     */
    @GetMapping("/flow/{memberId}")
    public Result<PageResult> flows(@PathVariable Integer memberId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) Integer type) {
        return Result.success(memberService.flows(memberId, type, page, pageSize));
    }

    /**
     * 查询全部等级规则
     */
    @GetMapping("/rules")
    public Result<List<MemberLevelRule>> rules() {
        return Result.success(memberService.listRules());
    }

    /**
     * 修改等级规则（门槛和折扣）
     */
    @PutMapping("/rules")
    public Result updateRules(@RequestBody List<MemberLevelRule> rules) {
        log.info("修改会员等级规则");
        memberService.updateRules(rules);
        return Result.success();
    }
}
