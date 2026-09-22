package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.BlindBoxOption;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 盲盒套餐选项 Mapper
 * 小白讲解：一个盲盒里有 3 个套餐选项，这里负责它们的新增、查询、清空。
 */
@Mapper
public interface BlindBoxOptionMapper {

    /**
     * 新增一个套餐选项
     */
    @Insert("insert into blind_box_option (box_id, option_name, dish_ids, dish_summary) " +
            "values (#{boxId}, #{optionName}, #{dishIds}, #{dishSummary})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BlindBoxOption option);

    /**
     * 根据id查选项
     */
    @Select("select * from blind_box_option where id=#{id}")
    BlindBoxOption getById(Integer id);

    /**
     * 查某个盲盒的全部套餐（抽卡时用）
     */
    @Select("select * from blind_box_option where box_id=#{boxId} order by id")
    List<BlindBoxOption> listByBoxId(Integer boxId);

    /**
     * 清空某个盲盒的全部套餐（编辑保存前先清再重建）
     */
    @Delete("delete from blind_box_option where box_id=#{boxId}")
    void deleteByBoxId(Integer boxId);
}
