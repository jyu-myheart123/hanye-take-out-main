package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.BlindBox;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 惊喜盲盒 Mapper
 * 小白讲解：盲盒本身的增删改查。盲盒里的套餐选项在 BlindBoxOptionMapper 单独管。
 */
@Mapper
public interface BlindBoxMapper {

    /**
     * 新增盲盒
     */
    @Insert("insert into blind_box (name, price, image, description, status) " +
            "values (#{name}, #{price}, #{image}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BlindBox blindBox);

    /**
     * 修改盲盒基本信息
     */
    @Update("update blind_box set name=#{name}, price=#{price}, image=#{image}, " +
            "description=#{description}, status=#{status} where id=#{id}")
    void update(BlindBox blindBox);

    /**
     * 根据id查盲盒
     */
    @Select("select * from blind_box where id=#{id}")
    BlindBox getById(Integer id);

    /**
     * 全部盲盒（配置列表用，最新的在前）
     */
    @Select("select * from blind_box order by update_time desc, id desc")
    List<BlindBox> listAll();

    /**
     * 删除盲盒
     */
    @Delete("delete from blind_box where id=#{id}")
    void delete(Integer id);
}
