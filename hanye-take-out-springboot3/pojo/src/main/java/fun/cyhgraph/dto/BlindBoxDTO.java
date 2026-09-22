package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 盲盒配置保存 DTO
 * 小白讲解：商家在"盲盒配置"里新建/编辑盲盒时，一次提交盲盒本身 + 它的3个套餐选项。
 */
@Data
public class BlindBoxDTO implements Serializable {

    /**
     * 盲盒id（新增时为空，编辑时有值）
     */
    private Integer id;
    /**
     * 盲盒名称
     */
    private String name;
    /**
     * 盲盒售价
     */
    private BigDecimal price;
    /**
     * 封面图
     */
    private String image;
    /**
     * 盲盒说明
     */
    private String description;
    /**
     * 状态：1启用 0停用
     */
    private Integer status;
    /**
     * 套餐选项列表（要求恰好3个，对应三张卡牌）
     */
    private List<OptionItem> options;

    /**
     * 单个套餐选项（内嵌在盲盒里一起提交）
     */
    @Data
    public static class OptionItem implements Serializable {
        /**
         * 选项id（编辑时可能有值）
         */
        private Integer id;
        /**
         * 套餐名（幸运套餐A）
         */
        private String optionName;
        /**
         * 菜品id逗号串（2,18,11）
         */
        private String dishIds;
        /**
         * 菜品摘要（鱼香肉丝 + 开心萝卜 + 米饭）
         */
        private String dishSummary;
    }
}
