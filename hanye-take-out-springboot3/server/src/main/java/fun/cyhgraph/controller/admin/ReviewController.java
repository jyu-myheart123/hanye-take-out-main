package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.ReviewDTO;
import fun.cyhgraph.dto.ReviewReplyDTO;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 客户评价管理接口（菜单最下面：堂食会员之下的"客户评价"）
 * 小白讲解：已完成的堂食订单可以在这里代顾客填写评价（菜品/口味/服务/出餐打分+好评+改进意见），
 * 后厨和管理者在"评价列表"查看、回复、隐藏不当评价。
 */
@RestController
@RequestMapping("/admin/review")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 待评价订单分页：已完成堂食单、还没评价的
     */
    @GetMapping("/pending")
    public Result<PageResult> pending(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) String phone) {
        return Result.success(reviewService.pagePending(page, pageSize, phone));
    }

    /**
     * 评价列表分页（可按关键词、状态筛选）
     */
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status) {
        return Result.success(reviewService.pageReviews(page, pageSize, keyword, status));
    }

    /**
     * 提交评价
     */
    @PostMapping
    public Result submit(@RequestBody ReviewDTO reviewDTO) {
        log.info("提交客户评价：{}", reviewDTO.getOrderId());
        reviewService.submit(reviewDTO);
        return Result.success();
    }

    /**
     * 商家回复评价
     */
    @PutMapping("/reply")
    public Result reply(@RequestBody ReviewReplyDTO reviewReplyDTO) {
        log.info("回复评价：{}", reviewReplyDTO.getId());
        reviewService.reply(reviewReplyDTO);
        return Result.success();
    }

    /**
     * 评价显示/隐藏
     */
    @PutMapping("/status/{id}")
    public Result onOff(@PathVariable Integer id) {
        reviewService.onOff(id);
        return Result.success();
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        reviewService.delete(id);
        return Result.success();
    }
}
