package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.BlindBoxDTO;
import fun.cyhgraph.dto.BlindBoxDrawDTO;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.BlindBoxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 惊喜盲盒管理接口（菜单：员工赏罚下面的"惊喜盲盒"）
 * 小白讲解：商家在这里配置盲盒；顾客付款后员工代点卡牌，抽中套餐自动生成正式订单。
 */
@RestController
@RequestMapping("/admin/blindbox")
@Slf4j
public class BlindBoxController {

    @Autowired
    private BlindBoxService blindBoxService;

    /**
     * 盲盒列表（含套餐选项）
     */
    @GetMapping("/list")
    public Result list() {
        return Result.success(blindBoxService.listAll());
    }

    /**
     * 保存盲盒配置（新增/编辑，含3个套餐）
     */
    @PostMapping
    public Result save(@RequestBody BlindBoxDTO dto) {
        log.info("保存盲盒配置：{}", dto.getName());
        blindBoxService.save(dto);
        return Result.success();
    }

    /**
     * 删除盲盒
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        blindBoxService.delete(id);
        return Result.success();
    }

    /**
     * 抽卡（付款后点卡牌时调用）：随机抽中套餐并生成订单，返回三张卡内容
     */
    @PostMapping("/draw")
    public Result draw(@RequestBody BlindBoxDrawDTO dto) {
        log.info("盲盒抽卡：boxId={}，orderType={}", dto.getBoxId(), dto.getOrderType());
        return Result.success(blindBoxService.draw(dto));
    }
}
