package fun.cyhgraph.mapper;

import com.github.pagehelper.Page;
import fun.cyhgraph.dto.MemberPageDTO;
import fun.cyhgraph.entity.Member;
import fun.cyhgraph.vo.MemberVO;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;

/**
 * 会员 Mapper
 * 小白讲解：会员表 member 的增删改查。
 * 重点：扣余额用的是"乐观锁"SQL——余额不够时一条都不会扣，防止并发把余额扣成负数。
 */
@Mapper
public interface MemberMapper {

    /**
     * 新增会员（消费自动建档 / 员工手动办卡都会走这里）
     */
    @Insert("insert into member (name, phone, balance, points, total_recharge, total_gift, total_consume, level, status) " +
            "values (#{name}, #{phone}, #{balance}, #{points}, #{totalRecharge}, #{totalGift}, #{totalConsume}, #{level}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Member member);

    /**
     * 根据id查会员
     */
    @Select("select * from member where id = #{id}")
    Member getById(Integer id);

    /**
     * 根据手机号查会员（开单时报手机号识别身份用）
     */
    @Select("select * from member where phone = #{phone}")
    Member getByPhone(String phone);

    /**
     * 分页查询会员，关联等级规则表带出等级名称和折扣
     */
    @Select("<script>" +
            "select m.*, r.level_name as levelName, r.discount as discount " +
            "from member m left join member_level_rule r on m.level = r.level " +
            "<where>" +
            "  <if test='name != null and name != \"\"'> and m.name like concat('%', #{name}, '%')</if>" +
            "  <if test='phone != null and phone != \"\"'> and m.phone like concat('%', #{phone}, '%')</if>" +
            "  <if test='level != null'> and m.level = #{level}</if>" +
            "</where>" +
            "order by m.create_time desc" +
            "</script>")
    Page<MemberVO> pageQuery(MemberPageDTO memberPageDTO);

    /**
     * 充值：余额增加（本金+赠送一起到账），并累计充值/赠送金额
     */
    @Update("update member set balance = balance + #{total}, " +
            "total_recharge = total_recharge + #{amount}, total_gift = total_gift + #{gift} " +
            "where id = #{id}")
    void recharge(@Param("id") Integer id,
                  @Param("amount") BigDecimal amount,
                  @Param("gift") BigDecimal gift,
                  @Param("total") BigDecimal total);

    /**
     * 乐观锁扣余额：只有当前余额足够时才扣得动，返回影响行数；返回0说明余额不足
     */
    @Update("update member set balance = balance - #{amount} where id = #{id} and balance >= #{amount}")
    int deductBalance(@Param("id") Integer id, @Param("amount") BigDecimal amount);

    /**
     * 积分增减（delta 正数增加、负数扣减）
     */
    @Update("update member set points = points + #{delta} where id = #{id} and points + #{delta} >= 0")
    int addPoints(@Param("id") Integer id, @Param("delta") Integer delta);

    /**
     * 累计消费增加（消费多少加多少，用来判断是否升级）
     */
    @Update("update member set total_consume = total_consume + #{amount} where id = #{id}")
    void addConsume(@Param("id") Integer id, @Param("amount") BigDecimal amount);

    /**
     * 更新会员等级（达到更高门槛时由Service调用）
     */
    @Update("update member set level = #{level} where id = #{id}")
    void updateLevel(@Param("id") Integer id, @Param("level") Integer level);

    /**
     * 修改会员姓名（手动办卡时可补录称呼）
     */
    @Update("update member set name = #{name} where id = #{id}")
    void updateName(@Param("id") Integer id, @Param("name") String name);

    /**
     * 启用/禁用切换
     */
    @Update("update member set status = IF(status = 0, 1, 0) where id = #{id}")
    void onOff(Integer id);
}
