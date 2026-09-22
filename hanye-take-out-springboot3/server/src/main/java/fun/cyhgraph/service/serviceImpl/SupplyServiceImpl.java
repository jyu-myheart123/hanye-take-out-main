package fun.cyhgraph.service.serviceImpl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.dto.PurchaseRecordDTO;
import fun.cyhgraph.entity.Material;
import fun.cyhgraph.entity.PurchaseRecord;
import fun.cyhgraph.entity.Supplier;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.mapper.MaterialMapper;
import fun.cyhgraph.mapper.PurchaseRecordMapper;
import fun.cyhgraph.mapper.SupplierMapper;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.SupplyService;
import fun.cyhgraph.vo.PurchaseRecordVO;
import fun.cyhgraph.vo.SupplierStatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应管理服务实现
 * 小白讲解：供应商和原材料是基础档案；每来一次货就写一条采购记录；
 * 今日/本月采购金额、准时率、合格率和月底应付，全部由采购记录实时统计，不存第二份数字。
 */
@Service
@Slf4j
public class SupplyServiceImpl implements SupplyService {

    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private PurchaseRecordMapper purchaseRecordMapper;

    // ================= 供应商 =================

    /**
     * 供应商列表 + 本月统计
     * 小白讲解：先把所有供应商查出来，再把本月的统计数字"对号入座"合并上去，
     * 本月没送货的供应商统计字段留空（前端显示"—"）。
     */
    @Override
    public List<SupplierStatVO> listSuppliersWithStats() {
        List<Supplier> suppliers = supplierMapper.listAll();
        YearMonth month = YearMonth.now();
        List<SupplierStatVO> stats = purchaseRecordMapper.statsBySupplier(
                month.atDay(1).atStartOfDay(),
                month.plusMonths(1).atDay(1).atStartOfDay());
        Map<Integer, SupplierStatVO> statMap = stats.stream()
                .collect(Collectors.toMap(SupplierStatVO::getId, s -> s));

        List<SupplierStatVO> result = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            SupplierStatVO row = new SupplierStatVO();
            // 拷贝供应商基本信息
            BeanCopy(supplier, row);
            SupplierStatVO stat = statMap.get(supplier.getId());
            if (stat != null) {
                row.setMonthCount(stat.getMonthCount());
                row.setMonthAmount(stat.getMonthAmount());
                row.setOnTimeRate(stat.getOnTimeRate());
                row.setQualifiedRate(stat.getQualifiedRate());
            }
            result.add(row);
        }
        // 本月送货金额高的排前面，方便查看
        result.sort((a, b) -> {
            BigDecimal va = a.getMonthAmount() == null ? BigDecimal.ZERO : a.getMonthAmount();
            BigDecimal vb = b.getMonthAmount() == null ? BigDecimal.ZERO : b.getMonthAmount();
            return vb.compareTo(va);
        });
        return result;
    }

    /**
     * 保存供应商（新增或修改）
     */
    @Override
    public void saveSupplier(Supplier supplier) {
        if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new OrderBusinessException("请填写供应商名称");
        }
        supplier.setName(supplier.getName().trim());
        supplier.setStatus(supplier.getStatus() == null ? 1 : supplier.getStatus());
        if (supplier.getId() == null) {
            supplierMapper.insert(supplier);
        } else {
            supplierMapper.update(supplier);
        }
    }

    /**
     * 删除供应商：已经有采购记录的不允许删（历史记录要留痕），建议改为停用
     */
    @Override
    public void deleteSupplier(Integer id) {
        if (supplierMapper.getById(id) == null) {
            throw new OrderBusinessException("供应商不存在");
        }
        PageHelper.startPage(1, 1);
        Page<PurchaseRecordVO> page = purchaseRecordMapper.pageQuery(id, null, null, null);
        if (!page.getResult().isEmpty()) {
            throw new OrderBusinessException("该供应商已有采购记录，不能删除（保留历史账目），可改为停用");
        }
        supplierMapper.delete(id);
    }

    // ================= 原材料 =================

    /**
     * 原材料列表
     */
    @Override
    public List<Material> listMaterials() {
        return materialMapper.listAll();
    }

    /**
     * 保存原材料（新增或修改）
     */
    @Override
    public void saveMaterial(Material material) {
        if (material.getName() == null || material.getName().trim().isEmpty()) {
            throw new OrderBusinessException("请填写原材料名称");
        }
        material.setName(material.getName().trim());
        material.setStatus(material.getStatus() == null ? 1 : material.getStatus());
        if (material.getUnit() == null || material.getUnit().trim().isEmpty()) {
            material.setUnit("斤");
        }
        if (material.getId() == null) {
            materialMapper.insert(material);
        } else {
            materialMapper.update(material);
        }
    }

    /**
     * 删除原材料：已被采购记录引用的不允许删
     */
    @Override
    public void deleteMaterial(Integer id) {
        if (materialMapper.getById(id) == null) {
            throw new OrderBusinessException("原材料不存在");
        }
        PageHelper.startPage(1, 1);
        Page<PurchaseRecordVO> page = purchaseRecordMapper.pageQuery(null, id, null, null);
        if (!page.getResult().isEmpty()) {
            throw new OrderBusinessException("该原材料已有采购记录，不能删除，可改为停用");
        }
        materialMapper.delete(id);
    }

    // ================= 采购记录 =================

    /**
     * 新增采购记录
     * 小白讲解：合计金额由后端计算（数量×单价），防止前端算错；
     * 准时/合格没勾选时默认都是"是"；到货时间没填就用当前时间。
     */
    @Override
    public void addPurchase(PurchaseRecordDTO dto) {
        if (dto.getSupplierId() == null) {
            throw new OrderBusinessException("请选择供应商");
        }
        if (dto.getMaterialId() == null) {
            throw new OrderBusinessException("请选择原材料");
        }
        if (supplierMapper.getById(dto.getSupplierId()) == null) {
            throw new OrderBusinessException("供应商不存在");
        }
        if (materialMapper.getById(dto.getMaterialId()) == null) {
            throw new OrderBusinessException("原材料不存在");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("请填写正确的采购数量");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderBusinessException("请填写正确的单价");
        }

        PurchaseRecord record = new PurchaseRecord();
        record.setSupplierId(dto.getSupplierId());
        record.setMaterialId(dto.getMaterialId());
        record.setQuantity(dto.getQuantity());
        record.setUnitPrice(dto.getUnitPrice());
        // 合计金额 = 数量 × 单价，后端说了算
        record.setTotalAmount(dto.getQuantity().multiply(dto.getUnitPrice()));
        record.setIsOnTime(dto.getIsOnTime() == null ? 1 : dto.getIsOnTime());
        record.setIsQualified(dto.getIsQualified() == null ? 1 : dto.getIsQualified());
        record.setPurchaseTime(dto.getPurchaseTime() == null ? LocalDateTime.now() : dto.getPurchaseTime());
        record.setRemark(dto.getRemark());
        purchaseRecordMapper.insert(record);
        log.info("采购记录已登记：供应商id={}，原料id={}，金额 ¥{}",
                record.getSupplierId(), record.getMaterialId(), record.getTotalAmount());
    }

    /**
     * 采购记录分页
     */
    @Override
    public PageResult purchasePage(int page, int pageSize, Integer supplierId, Integer materialId) {
        PageHelper.startPage(page, pageSize);
        Page<PurchaseRecordVO> pageResult = purchaseRecordMapper.pageQuery(supplierId, materialId, null, null);
        return new PageResult(pageResult.getTotal(), pageResult.getResult());
    }

    /**
     * 月底对账
     * 小白讲解：把"2026-09"这样的月份翻译成 9月1日0点 ~ 10月1日0点，
     * 一句分组 SQL 算出每个供应商该月送货单数、金额、准时率、合格率，店主照着付款。
     */
    @Override
    public List<SupplierStatVO> monthStatement(String month) {
        YearMonth ym;
        try {
            ym = (month == null || month.trim().isEmpty())
                    ? YearMonth.now() : YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw new OrderBusinessException("月份格式不正确，应为 yyyy-MM（如 2026-09）");
        }
        LocalDateTime begin = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        List<SupplierStatVO> list = purchaseRecordMapper.statsBySupplier(begin, end);

        // 把供应商名称补上（stats SQL 只带了 supplier_id）
        for (SupplierStatVO row : list) {
            Supplier supplier = supplierMapper.getById(row.getId());
            if (supplier != null) {
                row.setName(supplier.getName());
                row.setContactPerson(supplier.getContactPerson());
                row.setPhone(supplier.getPhone());
            }
        }
        // 应付金额从高到低
        list.sort((a, b) -> b.getMonthAmount().compareTo(a.getMonthAmount()));
        return list;
    }

    /**
     * 采购概览：统计今日和本月的采购金额与单数
     * 小白讲解：一天的边界是"今天0点 ~ 明天0点"，一月的边界是"1号0点 ~ 下月1号0点"。
     */
    @Override
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        Map<String, Object> todayStat = purchaseRecordMapper.sumByRange(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        Map<String, Object> monthStat = purchaseRecordMapper.sumByRange(
                today.withDayOfMonth(1).atStartOfDay(),
                today.withDayOfMonth(1).plusMonths(1).atStartOfDay());

        Map<String, Object> result = new HashMap<>();
        result.put("todayAmount", todayStat.get("amount"));
        result.put("todayCount", todayStat.get("cnt"));
        result.put("monthAmount", monthStat.get("amount"));
        result.put("monthCount", monthStat.get("cnt"));
        return result;
    }

    /**
     * 小工具：属性拷贝（手写个本地方法，避免漏 import BeanUtils）
     */
    private void BeanCopy(Object source, Object target) {
        org.springframework.beans.BeanUtils.copyProperties(source, target);
    }
}
