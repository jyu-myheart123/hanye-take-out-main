package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 手动新增会员 DTO
 * 小白讲解：顾客没消费也想先办会员卡时，员工用手机号手动建档
 */
@Data
public class MemberAddDTO implements Serializable {

    /**
     * 会员姓名（选填）
     */
    private String name;
    /**
     * 手机号（必填，会员的唯一标识）
     */
    private String phone;
}
