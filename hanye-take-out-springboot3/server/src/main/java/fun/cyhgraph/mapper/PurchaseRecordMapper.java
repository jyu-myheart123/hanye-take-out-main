package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.entity.PurchaseRecord;
import fun.cyhgraph.vo.PurchaseRecordVO;
import fun.cyhgraph.vo.SupplierStatVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购记录 Mapper
 * 小白讲解：采购记录的新增、分页查询，以及供应商统计（准时率、合格率、月度金额）。
 */
@Mapper
public interface PurchaseRecordMapper {

    /**
     * 新增一条采购记录
     */
    @Insert("insert into purchase_record (supplier_id, material_id, quantity, unit_price, total_amount, " +
            "is_on_time, is_qualified, purchase_time, remark) " +
            "values (#{supplierId}, #{materialId}, #{quantity}, #{unitPrice}, #{totalAmount}, " +
            "#{isOnTime}, #{isQualified}, #{purchaseTime}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PurchaseRecord record);

    /**
     * 分页查询采购记录（连表带供应商名、原料名、单位），可按供应商、原料、时间范围筛选
     */
    @Select("<script>" +
            "select p.*, s.name as supplierName, m.name as materialName, m.unit as materialUnit " +
            "from purchase_record p " +
            "left join supplier s on p.supplier_id = s.id " +
            "left join material m on p.material_id = m.id " +
            "<where>" +
            "  <if test='supplierId != null'> and p.supplier_id = #{supplierId}</if>" +
            "  <if test='materialId != null'> and p.material_id = #{materialId}</if>" +
            "  <if test='beginTime != null'> and p.purchase_time &gt;= #{beginTime}</if>" +
            "  <if test='endTime != null'> and p.purchase_time &lt;= #{endTime}</if>" +
            "</where>" +
            "order by p.purchase_time desc, p.id desc" +
            "</script>")
    Page<PurchaseRecordVO> pageQuery(@Param("supplierId") Integer supplierId,
                                     @Param("materialId") Integer materialId,
                                     @Param("beginTime") LocalDateTime beginTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计一段时间内每个供应商的送货单数、金额、准时率、合格率。
     * 小白讲解：按供应商分组，COUNT 数单数、SUM 算金额，
     * 准时率 = 准时单数 ÷ 总单数 × 100，合格率同理。
     */
    @Select("select p.supplier_id as id, " +
            "       count(*) as monthCount, " +
            "       round(sum(p.total_amount), 2) as monthAmount, " +
            "       round(sum(p.is_on_time) / count(*) * 100, 2) as onTimeRate, " +
            "       round(sum(p.is_qualified) / count(*) * 100, 2) as qualifiedRate " +
            "from purchase_record p " +
            "where p.purchase_time >= #{beginTime} and p.purchase_time < #{endTime} " +
            "group by p.supplier_id")
    List<SupplierStatVO> statsBySupplier(@Param("beginTime") LocalDateTime beginTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 统计一段时间内的采购总金额与总单数（用于首页"今日/本月采购"卡片）
     */
    @Select("select ifnull(sum(total_amount), 0) as amount, count(*) as cnt " +
            "from purchase_record where purchase_time >= #{beginTime} and purchase_time < #{endTime}")
    Map<String, Object> sumByRange(@Param("beginTime") LocalDateTime beginTime,
                                   @Param("endTime") LocalDateTime endTime);
}
