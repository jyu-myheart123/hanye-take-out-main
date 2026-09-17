package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 堂食会员实体（手机号会员 + 储值余额 + 积分 + 等级）
 * 小白讲解：老客户到店报手机号就能识别身份，余额可以充值，消费能攒积分，
 * 累计消费越高等级越高、折扣越大。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 会员等级常量 =====
    public static final Integer LEVEL_SILVER = 1;  // 银卡
    public static final Integer LEVEL_GOLD = 2;    // 金卡
    public static final Integer LEVEL_DIAMOND = 3; // 钻石

    private Integer id;
    /**
     * 会员姓名（选填，没填就用"手机号尾号"称呼）
     */
    private String name;
    /**
     * 手机号（会员的唯一标识，一个手机号只能注册一个会员）
     */
    private String phone;
    /**
     * 储值余额（充值送的钱也在里面，单位：元）
     */
    private BigDecimal balance;
    /**
     * 当前积分（消费1元积1分，100分可抵5元）
     */
    private Integer points;
    /**
     * 累计充值本金（不含赠送，方便对账）
     */
    private BigDecimal totalRecharge;
    /**
     * 累计赠送金额（如充200送20里的20）
     */
    private BigDecimal totalGift;
    /**
     * 累计消费金额（达到门槛自动升级会员等级）
     */
    private BigDecimal totalConsume;
    /**
     * 会员等级：1银卡 2金卡 3钻石
     */
    private Integer level;
    /**
     * 状态：0禁用 1正常
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
