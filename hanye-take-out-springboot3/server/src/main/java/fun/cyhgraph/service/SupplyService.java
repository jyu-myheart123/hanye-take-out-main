package fun.cyhgraph.service;

import fun.cyhgraph.dto.PurchaseRecordDTO;
import fun.cyhgraph.entity.Material;
import fun.cyhgraph.entity.Supplier;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.vo.SupplierStatVO;

import java.util.List;
import java.util.Map;

/**
 * 供应管理服务接口
 * 小白讲解：管供应商档案、原材料档案、采购记录，以及"本月统计、月底对账"。
 */
public interface SupplyService {

    /**
     * 供应商列表（附带本月送货单数、金额、准时率、合格率）
     */
    List<SupplierStatVO> listSuppliersWithStats();

    /**
     * 新增/修改供应商（有id=修改）
     */
    void saveSupplier(Supplier supplier);

    /**
     * 删除供应商
     */
    void deleteSupplier(Integer id);

    /**
     * 原材料列表
     */
    List<Material> listMaterials();

    /**
     * 新增/修改原材料
     */
    void saveMaterial(Material material);

    /**
     * 删除原材料
     */
    void deleteMaterial(Integer id);

    /**
     * 新增一条采购记录（合计金额后端自动算）
     */
    void addPurchase(PurchaseRecordDTO dto);

    /**
     * 采购记录分页（可按供应商/原材料筛选）
     */
    PageResult purchasePage(int page, int pageSize, Integer supplierId, Integer materialId);

    /**
     * 月底对账：month 格式 yyyy-MM（默认本月），返回每个供应商的送货单数与应付金额
     */
    List<SupplierStatVO> monthStatement(String month);

    /**
     * 采购概览：今日/本月的采购金额与单数
     */
    Map<String, Object> overview();
}
