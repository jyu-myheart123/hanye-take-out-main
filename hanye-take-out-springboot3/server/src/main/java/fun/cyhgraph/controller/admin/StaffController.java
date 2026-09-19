package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.StaffRatingDTO;
import fun.cyhgraph.dto.StaffRewardDTO;
import fun.cyhgraph.dto.StaffTipDTO;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.StaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工赏罚管理接口（菜单：客户评价下面的"员工赏罚"）
 * 小白讲解：这个控制器给前端提供三类接口——
 * 1.顾客服务评分与小费打赏；2.员工星光排行榜（今日/近3日/近7日/近一月）；
 * 3.评分记录、赏罚流水查询，以及店主（管理员）的奖励/惩戒。
 */
@RestController
@RequestMapping("/admin/staff")
@Slf4j
public class StaffController {

    @Autowired
    private StaffService staffService;

    /**
     * 在职员工列表（评分时选择服务员工）
     */
    @GetMapping("/employees")
    public Result employees() {
        return Result.success(staffService.listActiveEmployees());
    }

    /**
     * 提交员工服务评分。返回评分id，前端接着弹"是否给小费"
     */
    @PostMapping("/rating")
    public Result<Map<String, Object>> submitRating(@RequestBody StaffRatingDTO dto) {
        log.info("提交员工服务评分：员工id={}", dto.getEmployeeId());
        Integer ratingId = staffService.submitRating(dto);
        // 把评分id包成 {id: x} 返回，前端取值更方便
        Map<String, Object> data = new HashMap<>();
        data.put("id", ratingId);
        return Result.success(data);
    }

    /**
     * 顾客打赏小费（评分弹窗点"给小费"后调用）
     */
    @PostMapping("/tip")
    public Result tip(@RequestBody StaffTipDTO dto) {
        log.info("顾客打赏小费：评分id={}，金额={}", dto.getRatingId(), dto.getAmount());
        staffService.tip(dto);
        return Result.success();
    }

    /**
     * 星光排行榜：range = today(默认今日) / 3d / 7d / month
     */
    @GetMapping("/rank")
    public Result rank(@RequestParam(defaultValue = "today") String range) {
        return Result.success(staffService.rank(range));
    }

    /**
     * 评分记录分页（可按员工、状态、关键词筛选）
     */
    @GetMapping("/rating/page")
    public Result<PageResult> ratingPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) Integer employeeId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) String keyword) {
        return Result.success(staffService.ratingPage(page, pageSize, employeeId, status, keyword));
    }

    /**
     * 评分显示/隐藏
     */
    @PutMapping("/rating/status/{id}")
    public Result ratingOnOff(@PathVariable Integer id) {
        staffService.ratingOnOff(id);
        return Result.success();
    }

    /**
     * 删除评分
     */
    @DeleteMapping("/rating/{id}")
    public Result ratingDelete(@PathVariable Integer id) {
        staffService.ratingDelete(id);
        return Result.success();
    }

    /**
     * 赏罚流水分页（可按员工、类型筛选）
     */
    @GetMapping("/reward/page")
    public Result<PageResult> rewardPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) Integer employeeId,
                                         @RequestParam(required = false) Integer type) {
        return Result.success(staffService.rewardPage(page, pageSize, employeeId, type));
    }

    /**
     * 店主奖励/惩戒（后端校验只有管理员能操作）
     */
    @PostMapping("/reward")
    public Result addReward(@RequestBody StaffRewardDTO dto) {
        log.info("店主赏罚：员工id={}，类型={}", dto.getEmployeeId(), dto.getType());
        staffService.addReward(dto);
        return Result.success();
    }
}
