package fun.cyhgraph.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商家回复评价 DTO
 */
@Data
public class ReviewReplyDTO implements Serializable {

    /**
     * 评价id
     */
    private Integer id;
    /**
     * 回复内容
     */
    private String reply;
}
