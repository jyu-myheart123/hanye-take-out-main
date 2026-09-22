package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.PurchaseRecordDTO;
import fun.cyhgraph.entity.Material;
import fun.cyhgraph.entity.Supplier;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.SupplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 供应管理接口（菜单：惊喜盲盒下面的"供应管理"）
 * 小白讲解：供应商档案、原材料档案、采购入库记录、月度对账四类能力都在这里。
 */
@RestController
@RequestMapping("/admin/supply")
@Slf4j
public class SupplyController {

    @Autowired
    private SupplyService supplyService;

    // ---------- 供应商 ----------

    /**
     * 供应商列表（含本月统计）
     */
    @GetMapping("/supplier/list")
    public Result supplierList() {
        return Result.success(supplyService.listSuppliersWithStats());
    }

    /**
     * 保存供应商（新增/编辑）
     */
    @PostMapping("/supplier")
    public Result saveSupplier(@RequestBody Supplier supplier) {
        supplyService.saveSupplier(supplier);
        return Result.success();
    }

    /**
     * 删除供应商
     */
    @DeleteMapping("/supplier/{id}")
    public Result deleteSupplier(@PathVariable Integer id) {
        supplyService.deleteSupplier(id);
        return Result.success();
    }

    // ---------- 原材料 ----------

    /**
     * 原材料列表
     */
    @GetMapping("/material/list")
    public Result materialList() {
        return Result.success(supplyService.listMaterials());
    }

    /**
     * 保存原材料（新增/编辑）
     */
    @PostMapping("/material")
    public Result saveMaterial(@RequestBody Material material) {
        supplyService.saveMaterial(material);
        return Result.success();
    }

    /**
     * 删除原材料
     */
    @DeleteMapping("/material/{id}")
    public Result deleteMaterial(@PathVariable Integer id) {
        supplyService.deleteMaterial(id);
        return Result.success();
    }

    // ---------- 采购记录 ----------

    /**
     * 采购记录分页
     */
    @GetMapping("/purchase/page")
    public Result<PageResult> purchasePage(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Integer supplierId,
                                           @RequestParam(required = false) Integer materialId) {
        return Result.success(supplyService.purchasePage(page, pageSize, supplierId, materialId));
    }

    /**
     * 新增采购记录
     */
    @PostMapping("/purchase")
    public Result addPurchase(@RequestBody PurchaseRecordDTO dto) {
        supplyService.addPurchase(dto);
        return Result.success();
    }

    // ---------- 月底对账 ----------

    /**
     * 月度对账：month=yyyy-MM，默认本月
     */
    @GetMapping("/statement")
    public Result statement(@RequestParam(required = false) String month) {
        return Result.success(supplyService.monthStatement(month));
    }

    /**
     * 采购概览：今日/本月采购金额与单数
     */
    @GetMapping("/overview")
    public Result overview() {
        return Result.success(supplyService.overview());
    }
}
