package fun.cyhgraph.controller.admin;

import fun.cyhgraph.entity.Promotion;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 营销活动管理接口（员工管理菜单下的"营销活动"页面）
 * 小白讲解：商家在这个页面配置满减、折扣、第二份半价、买一送一活动，
 * 配置后开单页会自动按最优惠的活动算账，不需要收银员手动算。
 */
@RestController
@RequestMapping("/admin/promotion")
@Slf4j
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    /**
     * 分页查询活动（可按名称、状态、类型筛选）
     */
    @GetMapping("/page")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) Integer type) {
        log.info("营销活动分页查询：page={}, pageSize={}, name={}, status={}, type={}", page, pageSize, name, status, type);
        return Result.success(promotionService.pageQuery(page, pageSize, name, status, type));
    }

    /**
     * 根据id查活动详情（编辑回显）
     */
    @GetMapping("/{id}")
    public Result<Promotion> getById(@PathVariable Integer id) {
        return Result.success(promotionService.getById(id));
    }

    /**
     * 查询当前正在生效的活动（开单页顶部横幅提示用）
     */
    @GetMapping("/active")
    public Result<List<Promotion>> active() {
        return Result.success(promotionService.listActive());
    }

    /**
     * 新增活动
     */
    @PostMapping
    public Result save(@RequestBody Promotion promotion) {
        log.info("新增营销活动：{}", promotion.getName());
        promotionService.save(promotion);
        return Result.success();
    }

    /**
     * 修改活动
     */
    @PutMapping
    public Result update(@RequestBody Promotion promotion) {
        log.info("修改营销活动：{}", promotion.getId());
        promotionService.update(promotion);
        return Result.success();
    }

    /**
     * 启用/停用活动
     */
    @PutMapping("/status/{id}")
    public Result onOff(@PathVariable Integer id) {
        log.info("切换营销活动状态：{}", id);
        promotionService.onOff(id);
        return Result.success();
    }

    /**
     * 删除活动
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除营销活动：{}", id);
        promotionService.deleteById(id);
        return Result.success();
    }
}
