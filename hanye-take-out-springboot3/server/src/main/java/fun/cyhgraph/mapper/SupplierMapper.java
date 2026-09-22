package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.Supplier;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 供应商 Mapper
 * 小白讲解：供应商档案的增删改查。
 */
@Mapper
public interface SupplierMapper {

    /**
     * 新增供应商
     */
    @Insert("insert into supplier (name, contact_person, phone, address, main_category, status, remark) " +
            "values (#{name}, #{contactPerson}, #{phone}, #{address}, #{mainCategory}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Supplier supplier);

    /**
     * 修改供应商
     */
    @Update("update supplier set name=#{name}, contact_person=#{contactPerson}, phone=#{phone}, " +
            "address=#{address}, main_category=#{mainCategory}, status=#{status}, remark=#{remark} where id=#{id}")
    void update(Supplier supplier);

    /**
     * 根据id查供应商
     */
    @Select("select * from supplier where id=#{id}")
    Supplier getById(Integer id);

    /**
     * 全部供应商（最新的在前）
     */
    @Select("select * from supplier order by id")
    List<Supplier> listAll();

    /**
     * 删除供应商
     */
    @Delete("delete from supplier where id=#{id}")
    void delete(Integer id);
}
