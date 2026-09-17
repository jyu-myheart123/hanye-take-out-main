package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.entity.MemberFlow;
import org.apache.ibatis.annotations.*;

/**
 * 会员流水 Mapper
 * 小白讲解：会员余额/积分的每次变动都写一条流水，会员页"消费记录"就查这张表
 */
@Mapper
public interface MemberFlowMapper {

    /**
     * 新增一条流水
     */
    @Insert("insert into member_flow (member_id, type, amount, points, balance_after, points_after, order_id, remark) " +
            "values (#{memberId}, #{type}, #{amount}, #{points}, #{balanceAfter}, #{pointsAfter}, #{orderId}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MemberFlow memberFlow);

    /**
     * 分页查询某个会员的流水（可按类型筛选：消费/充值/积分等），最新的在前面
     */
    @Select("<script>" +
            "select * from member_flow " +
            "<where>" +
            "  member_id = #{memberId}" +
            "  <if test='type != null'> and type = #{type}</if>" +
            "</where>" +
            "order by create_time desc, id desc" +
            "</script>")
    Page<MemberFlow> pageByMember(@Param("memberId") Integer memberId, @Param("type") Integer type);
}
