package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.Material;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 原材料 Mapper
 * 小白讲解：原材料档案的增删改查。
 */
@Mapper
public interface MaterialMapper {

    /**
     * 新增原材料
     */
    @Insert("insert into material (name, unit, reference_price, category, supplier_id, status) " +
            "values (#{name}, #{unit}, #{referencePrice}, #{category}, #{supplierId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Material material);

    /**
     * 修改原材料
     */
    @Update("update material set name=#{name}, unit=#{unit}, reference_price=#{referencePrice}, " +
            "category=#{category}, supplier_id=#{supplierId}, status=#{status} where id=#{id}")
    void update(Material material);

    /**
     * 根据id查原材料
     */
    @Select("select * from material where id=#{id}")
    Material getById(Integer id);

    /**
     * 全部原材料
     */
    @Select("select * from material order by category, id")
    List<Material> listAll();

    /**
     * 删除原材料
     */
    @Delete("delete from material where id=#{id}")
    void delete(Integer id);
}
