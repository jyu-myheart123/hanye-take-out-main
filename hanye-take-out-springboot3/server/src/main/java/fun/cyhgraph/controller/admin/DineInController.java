package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.DineInOrderDTO;
import fun.cyhgraph.entity.Category;
import fun.cyhgraph.entity.Dish;
import fun.cyhgraph.mapper.CategoryMapper;
import fun.cyhgraph.mapper.DishMapper;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.OrderService;
import fun.cyhgraph.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 堂食下单相关接口（员工在后台为到店客户代下单）
 * 小白讲解：老客户不想用小程序、直接到店里点菜时，
 * 员工就在这个页面勾选菜品、核算价格、一键开单，订单直接进后厨接单队列。
 */
@RestController
@RequestMapping("/admin/dinein")
@Slf4j
public class DineInController {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private OrderService orderService;

    /**
     * 查询所有启用中的分类（供开单页左侧分类标签使用）
     */
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        log.info("堂食开单：查询启用的分类列表");
        // type 传 null 表示查询全部启用分类（菜品分类 + 套餐分类）
        List<Category> list = categoryMapper.getList(null);
        return Result.success(list);
    }

    /**
     * 根据分类id查询启售中的菜品（供开单页点菜使用）
     * @param categoryId 分类id，不传则查全部启售菜品
     */
    @GetMapping("/dishes")
    public Result<List<Dish>> dishes(@RequestParam(required = false) Integer categoryId) {
        log.info("堂食开单：查询启售菜品，分类id={}", categoryId);
        Dish dish = new Dish();
        dish.setStatus(1); // 只查启售的，停售的菜不能点
        dish.setCategoryId(categoryId);
        List<Dish> list = dishMapper.getList(dish);
        return Result.success(list);
    }

    /**
     * 提交堂食订单
     * 小白讲解：前端只传"点了哪些菜、各几份"，价格全部由后端重新核算，
     * 防止前端篡改金额
     */
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody DineInOrderDTO dineInOrderDTO) {
        log.info("堂食开单信息：{}", dineInOrderDTO);
        OrderSubmitVO vo = orderService.dineInSubmit(dineInOrderDTO);
        return Result.success(vo);
    }
}
