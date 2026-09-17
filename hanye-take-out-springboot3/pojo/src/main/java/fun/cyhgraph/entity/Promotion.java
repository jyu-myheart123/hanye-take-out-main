package fun.cyhgraph.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销活动实体
 * 小白讲解：商家在"营销活动"页面配置的一条优惠规则，比如"满30减5""全场8.8折"。
 * type 决定是哪种优惠，具体参数放在 ruleJson（数据库里以 JSON 字符串保存）。
 */
@Data
public class Promotion implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 活动类型常量，代码里用常量比直接写 1/2/3/4 更好懂 =====
    public static final Integer FULL_REDUCTION = 1;  // 满减
    public static final Integer DISCOUNT = 2;        // 折扣
    public static final Integer SECOND_HALF = 3;     // 第二份半价
    public static final Integer BUY_ONE_GET_ONE = 4; // 买一送一

    // ===== 适用范围 =====
    public static final Integer SCOPE_ALL = 0;       // 全场
    public static final Integer SCOPE_CATEGORY = 1;  // 指定分类
    public static final Integer SCOPE_DISH = 2;      // 指定菜品

    private Integer id;
    /**
     * 活动名称，如"午市满减活动"
     */
    private String name;
    /**
     * 活动类型：1满减 2折扣 3第二份半价 4买一送一
     */
    private Integer type;
    /**
     * 规则JSON字符串（小白讲解：就是把优惠参数转成一段文本存进数据库）
     * 满减：{"tiers":[{"threshold":30,"reduce":5}]}
     * 折扣：{"discount":8.8} 表示8.8折
     */
    private String ruleJson;
    /**
     * 适用范围：0全场 1指定分类 2指定菜品
     */
    private Integer scopeType;
    /**
     * 适用范围的id列表，数据库里用逗号拼接存成字符串，如 "3,7,9"
     */
    private String scopeIds;
    /**
     * 活动开始时间
     */
    private LocalDateTime beginTime;
    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;
    /**
     * 状态：0停用 1启用
     */
    private Integer status;
    /**
     * 活动说明（给员工看的备注）
     */
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== 下面三个字段不映射数据库列，专门用来和前端互传表单数据 =====

    /**
     * 满减档位列表（仅满减活动使用），保存时由Service转成 ruleJson
     */
    private List<Tier> tiers;
    /**
     * 折扣值（仅折扣活动使用），8.8 表示8.8折，保存时由Service转成 ruleJson
     */
    private BigDecimal discount;
    /**
     * 适用分类/菜品id列表，保存时由Service拼成逗号字符串 scopeIds
     */
    private List<Integer> scopeIdList;

    /**
     * 满减的一档：满 threshold 元，减 reduce 元
     */
    @Data
    public static class Tier implements Serializable {
        /**
         * 满足门槛，如30表示满30元
         */
        private BigDecimal threshold;
        /**
         * 减免金额，如5表示减5元
         */
        private BigDecimal reduce;
    }
}
