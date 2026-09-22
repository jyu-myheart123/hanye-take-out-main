package fun.cyhgraph.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盲盒套餐选项实体
 * 小白讲解：一个盲盒里放 3 个这样的"套餐选项"，每个选项就是一套确定的菜（如一荤一素一饭）。
 * dish_ids 存菜品id的逗号串（如 2,18,11），dish_summary 存菜名摘要方便直接展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlindBoxOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 所属盲盒id
     */
    private Integer boxId;
    /**
     * 套餐名（如"幸运套餐A"），抽卡时展示
     */
    private String optionName;
    /**
     * 菜品id逗号串（如 2,18,11）
     */
    private String dishIds;
    /**
     * 菜品摘要（鱼香肉丝 + 开心萝卜 + 米饭），冗余存一份方便展示
     */
    private String dishSummary;
    private LocalDateTime createTime;
}
