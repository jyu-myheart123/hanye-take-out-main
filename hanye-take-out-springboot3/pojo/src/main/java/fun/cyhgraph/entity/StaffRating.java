package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工服务评分实体
 * 小白讲解：顾客吃完饭，员工照着顾客的反馈代填一张"服务评分卡"：
 * 给服务员工的服务态度打星、给"推荐的菜品合不合心意"打星、写几句评语；
 * 评分之后顾客还可以给小费打赏。员工攒"北极星积分"，按周/日参与排名发奖金。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRating implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 被评价的服务员工id
     */
    private Integer employeeId;
    /**
     * 员工姓名（冗余存一份，列表展示时不用再连表查员工）
     */
    private String employeeName;
    /**
     * 关联堂食订单id（选填，没关联订单也能评分）
     */
    private Integer orderId;
    /**
     * 桌号（选填，方便知道是哪桌给的反馈）
     */
    private String tableNo;
    /**
     * 顾客称呼（选填，如"王先生"）
     */
    private String customerName;
    /**
     * 服务态度评分：1-5 星（必填）
     */
    private Integer serviceScore;
    /**
     * 推荐指数：员工推荐的菜品合不合心意，1-5 星（选填）
     */
    private Integer recommendScore;
    /**
     * 评价内容/评语
     */
    private String content;
    /**
     * 顾客打赏小费金额（默认0，评分后弹窗决定是否给）
     */
    private BigDecimal tipAmount;
    /**
     * 本次评价获得的北极星积分（服务态度几星就得几分）
     */
    private Integer starPoints;
    /**
     * 状态：1有效（计入排名） 0隐藏（不当评价不参与排名）
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
