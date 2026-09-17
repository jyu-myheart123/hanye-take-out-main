package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.entity.Promotion;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销活动 Mapper
 * 小白讲解：营销活动表 promotion 的增删改查都在这里，
 * 带 <script> 的是动态 SQL（搜索条件可能传也可能不传）。
 */
@Mapper
public interface PromotionMapper {

    /**
     * 新增活动（规则已由Service转成 ruleJson 字符串）
     */
    @Insert("insert into promotion (name, type, rule_json, scope_type, scope_ids, begin_time, end_time, status, description) " +
            "values (#{name}, #{type}, #{ruleJson}, #{scopeType}, #{scopeIds}, #{beginTime}, #{endTime}, #{status}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Promotion promotion);

    /**
     * 修改活动（只改内容，启停单独走 onOff）
     */
    @Update("<script>" +
            "update promotion " +
            "<set>" +
            "  <if test='name != null'>name = #{name},</if>" +
            "  <if test='type != null'>type = #{type},</if>" +
            "  <if test='ruleJson != null'>rule_json = #{ruleJson},</if>" +
            "  <if test='scopeType != null'>scope_type = #{scopeType},</if>" +
            "  scope_ids = #{scopeIds}," +
            "  <if test='beginTime != null'>begin_time = #{beginTime},</if>" +
            "  <if test='endTime != null'>end_time = #{endTime},</if>" +
            "  <if test='description != null'>description = #{description},</if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    void update(Promotion promotion);

    /**
     * 根据id查活动
     */
    @Select("select * from promotion where id = #{id}")
    Promotion getById(Integer id);

    /**
     * 分页查询活动（可按名称模糊、状态、类型筛选）
     */
    @Select("<script>" +
            "select * from promotion " +
            "<where>" +
            "  <if test='name != null and name != \"\"'> and name like concat('%', #{name}, '%')</if>" +
            "  <if test='status != null'> and status = #{status}</if>" +
            "  <if test='type != null'> and type = #{type}</if>" +
            "</where>" +
            "order by update_time desc" +
            "</script>")
    Page<Promotion> pageQuery(@Param("name") String name,
                              @Param("status") Integer status,
                              @Param("type") Integer type);

    /**
     * 查询某一时刻正在生效的活动（启用中且在活动时间范围内），开单算价时调用
     */
    @Select("select * from promotion where status = 1 and begin_time <= #{now} and end_time >= #{now} " +
            "order by id asc")
    List<Promotion> listActive(LocalDateTime now);

    /**
     * 启用/停用切换（0变1、1变0）
     */
    @Update("update promotion set status = IF(status = 0, 1, 0) where id = #{id}")
    void onOff(Integer id);

    /**
     * 删除活动
     */
    @Delete("delete from promotion where id = #{id}")
    void delete(Integer id);
}
