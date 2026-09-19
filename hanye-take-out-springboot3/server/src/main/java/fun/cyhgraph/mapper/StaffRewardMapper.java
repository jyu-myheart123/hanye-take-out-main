package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.entity.StaffReward;
import fun.cyhgraph.vo.StaffRankVO;
import fun.cyhgraph.vo.StaffRewardVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工赏罚流水 Mapper
 * 小白讲解：流水表 staff_reward 的新增、分页查询，以及"星光排行榜"的统计聚合。
 */
@Mapper
public interface StaffRewardMapper {

    /**
     * 新增一条赏罚流水（顾客打赏 / 店主奖励 / 店主惩戒都走这里）
     */
    @Insert("insert into staff_reward (employee_id, type, amount, star_points, rating_id, order_id, reason, operator_id) " +
            "values (#{employeeId}, #{type}, #{amount}, #{starPoints}, #{ratingId}, #{orderId}, #{reason}, #{operatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(StaffReward staffReward);

    /**
     * 分页查询赏罚流水（连表带员工姓名），可按员工、类型筛选
     */
    @Select("<script>" +
            "select w.*, e.name as employeeName " +
            "from staff_reward w left join employee e on w.employee_id = e.id " +
            "<where>" +
            "  <if test='employeeId != null'> and w.employee_id = #{employeeId}</if>" +
            "  <if test='type != null'> and w.type = #{type}</if>" +
            "</where>" +
            "order by w.create_time desc, w.id desc" +
            "</script>")
    Page<StaffRewardVO> pageQuery(@Param("employeeId") Integer employeeId, @Param("type") Integer type);

    /**
     * 星光排行榜：统计 beginTime 之后每个员工的评价人数、平均分、北极星积分、小费、店主奖惩净额。
     * 只列出这段时间内"有有效评分"的员工，平均分高的在前。
     * 小白讲解：这是一句汇总 SQL——把同一个员工的多条评分 GROUP BY 成一行，
     * COUNT 数条数、AVG 求平均、SUM 求和；奖惩净额用一个子查询单独从流水表算。
     */
    @Select("select e.id as employeeId, e.name as employeeName, " +
            "       count(r.id) as ratingCount, " +
            "       coalesce(round(avg(r.service_score), 2), 0) as avgService, " +
            "       coalesce(round(avg(r.recommend_score), 2), 0) as avgRecommend, " +
            "       coalesce(sum(r.star_points), 0) + " +
            "         (select coalesce(sum(w.star_points), 0) from staff_reward w " +
            "           where w.employee_id = e.id and w.type in (2, 3) and w.create_time >= #{beginTime}) as starPoints, " +
            "       coalesce(sum(r.tip_amount), 0) as tipTotal, " +
            "       (select coalesce(sum(w.amount), 0) from staff_reward w " +
            "         where w.employee_id = e.id and w.type in (2, 3) and w.create_time >= #{beginTime}) as rewardNet " +
            "from staff_rating r join employee e on e.id = r.employee_id " +
            "where r.status = 1 and r.create_time >= #{beginTime} " +
            "group by e.id, e.name " +
            "order by avgService desc, starPoints desc, ratingCount desc")
    List<StaffRankVO> rankList(@Param("beginTime") LocalDateTime beginTime);
}
