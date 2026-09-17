package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.entity.Order;
import fun.cyhgraph.entity.Review;
import fun.cyhgraph.vo.ReviewVO;
import org.apache.ibatis.annotations.*;

/**
 * 客户评价 Mapper
 * 小白讲解：评价表 review 的增删改查，外加"哪些已完成堂食单还没评价"的查询
 */
@Mapper
public interface ReviewMapper {

    /**
     * 提交一条评价
     */
    @Insert("insert into review (order_id, order_number, table_no, member_id, customer_name, " +
            "dish_score, taste_score, service_score, speed_score, content, suggestion, reply, status) " +
            "values (#{orderId}, #{orderNumber}, #{tableNo}, #{memberId}, #{customerName}, " +
            "#{dishScore}, #{tasteScore}, #{serviceScore}, #{speedScore}, #{content}, #{suggestion}, #{reply}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Review review);

    /**
     * 根据订单id查评价（用来判断这单是否已经评价过）
     */
    @Select("select * from review where order_id = #{orderId}")
    Review getByOrderId(Integer orderId);

    /**
     * 根据id查评价
     */
    @Select("select * from review where id = #{id}")
    Review getById(Integer id);

    /**
     * 分页查询评价，关联订单表带下单时间和实付金额；可按关键词（好评/意见/回复）和状态筛选
     */
    @Select("<script>" +
            "select r.*, o.order_time as orderTime, o.amount as orderAmount " +
            "from review r left join orders o on r.order_id = o.id " +
            "<where>" +
            "  <if test='status != null'> and r.status = #{status}</if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    and (r.content like concat('%', #{keyword}, '%') " +
            "      or r.suggestion like concat('%', #{keyword}, '%') " +
            "      or r.reply like concat('%', #{keyword}, '%') " +
            "      or r.customer_name like concat('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "order by r.create_time desc, r.id desc" +
            "</script>")
    Page<ReviewVO> pageQuery(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 分页查询"待评价"的已完成堂食订单：堂食单(order_type=2)、已完成(status=5)、还没有评价
     */
    @Select("<script>" +
            "select * from orders o " +
            "where o.order_type = 2 and o.status = 5 " +
            "and not exists (select 1 from review r where r.order_id = o.id) " +
            "<if test='phone != null and phone != \"\"'> and o.phone like concat('%', #{phone}, '%')</if>" +
            "order by o.delivery_time desc, o.order_time desc" +
            "</script>")
    Page<Order> pagePendingReview(@Param("phone") String phone);

    /**
     * 商家回复评价
     */
    @Update("update review set reply = #{reply} where id = #{id}")
    void updateReply(@Param("id") Integer id, @Param("reply") String reply);

    /**
     * 评价显示/隐藏切换
     */
    @Update("update review set status = IF(status = 0, 1, 0) where id = #{id}")
    void onOff(Integer id);

    /**
     * 删除评价
     */
    @Delete("delete from review where id = #{id}")
    void delete(Integer id);
}
