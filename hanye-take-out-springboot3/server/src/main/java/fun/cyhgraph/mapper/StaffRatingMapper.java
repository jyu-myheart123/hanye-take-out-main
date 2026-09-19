package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.entity.Employee;
import fun.cyhgraph.entity.StaffRating;
import org.apache.ibatis.annotations.*;

/**
 * 员工服务评分 Mapper
 * 小白讲解：员工评分表 staff_rating 的增删改查，外加"在职员工下拉列表"的查询。
 */
@Mapper
public interface StaffRatingMapper {

    /**
     * 提交一条员工服务评分
     */
    @Insert("insert into staff_rating (employee_id, employee_name, order_id, table_no, customer_name, " +
            "service_score, recommend_score, content, tip_amount, star_points, status) " +
            "values (#{employeeId}, #{employeeName}, #{orderId}, #{tableNo}, #{customerName}, " +
            "#{serviceScore}, #{recommendScore}, #{content}, #{tipAmount}, #{starPoints}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(StaffRating staffRating);

    /**
     * 根据id查评分
     */
    @Select("select * from staff_rating where id = #{id}")
    StaffRating getById(Integer id);

    /**
     * 顾客打赏：把小费金额补到这条评分上
     */
    @Update("update staff_rating set tip_amount = #{tipAmount} where id = #{id}")
    void updateTip(@Param("id") Integer id, @Param("tipAmount") java.math.BigDecimal tipAmount);

    /**
     * 分页查询评分记录，可按员工、状态筛选；keyword 可搜评语/顾客称呼/桌号
     */
    @Select("<script>" +
            "select * from staff_rating " +
            "<where>" +
            "  <if test='employeeId != null'> and employee_id = #{employeeId}</if>" +
            "  <if test='status != null'> and status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    and (content like concat('%', #{keyword}, '%') " +
            "      or customer_name like concat('%', #{keyword}, '%') " +
            "      or table_no like concat('%', #{keyword}, '%') " +
            "      or employee_name like concat('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "order by create_time desc, id desc" +
            "</script>")
    Page<StaffRating> pageQuery(@Param("employeeId") Integer employeeId,
                                @Param("status") Integer status,
                                @Param("keyword") String keyword);

    /**
     * 评分显示/隐藏切换（隐藏后不计入排行榜）
     */
    @Update("update staff_rating set status = IF(status = 0, 1, 0) where id = #{id}")
    void onOff(Integer id);

    /**
     * 删除评分
     */
    @Delete("delete from staff_rating where id = #{id}")
    void delete(Integer id);

    /**
     * 在职员工简要列表（评分时下拉选择用；管理员排前面）
     */
    @Select("select id, name, role from employee where status = 1 order by role desc, id asc")
    java.util.List<Employee> listActiveEmployees();
}
