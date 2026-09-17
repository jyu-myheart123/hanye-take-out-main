package fun.cyhgraph.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 会员分页查询 DTO
 * 小白讲解：会员列表页的搜索条件——支持按姓名/手机号模糊搜、按等级筛选
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MemberPageDTO extends PageDTO implements Serializable {

    /**
     * 手机号模糊搜索
     */
    private String phone;
    /**
     * 等级筛选：1银卡 2金卡 3钻石，不传=全部
     */
    private Integer level;
}
