package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.MemberLevelRule;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员等级规则 Mapper
 * 小白讲解：银卡/金卡/钻石的升级门槛和折扣都存在这张表，商家可在后台自己改
 */
@Mapper
public interface MemberLevelRuleMapper {

    /**
     * 查询全部等级规则，按等级从低到高
     */
    @Select("select * from member_level_rule order by level asc")
    List<MemberLevelRule> listAll();

    /**
     * 根据等级查单条规则
     */
    @Select("select * from member_level_rule where level = #{level}")
    MemberLevelRule getByLevel(Integer level);

    /**
     * 统计规则数量（启动时判断要不要初始化默认规则）
     */
    @Select("select count(*) from member_level_rule")
    int count();

    /**
     * 初始化插入一条默认规则
     */
    @Insert("insert into member_level_rule (level, level_name, threshold_amount, discount) " +
            "values (#{level}, #{levelName}, #{thresholdAmount}, #{discount})")
    void insert(MemberLevelRule rule);

    /**
     * 修改某等级的门槛和折扣
     */
    @Update("update member_level_rule set level_name = #{levelName}, threshold_amount = #{thresholdAmount}, " +
            "discount = #{discount} where level = #{level}")
    void update(@Param("level") Integer level,
                @Param("levelName") String levelName,
                @Param("thresholdAmount") BigDecimal thresholdAmount,
                @Param("discount") BigDecimal discount);
}
