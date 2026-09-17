package fun.cyhgraph.service;

import fun.cyhgraph.dto.ReviewDTO;
import fun.cyhgraph.dto.ReviewReplyDTO;
import fun.cyhgraph.result.PageResult;

/**
 * 客户评价服务接口
 */
public interface ReviewService {

    /**
     * 待评价订单分页：只查"已完成的堂食单且还没评价"的
     */
    PageResult pagePending(int page, int pageSize, String phone);

    /**
     * 评价列表分页（后厨/管理者查看，可按关键词、状态筛选）
     */
    PageResult pageReviews(int page, int pageSize, String keyword, Integer status);

    /**
     * 提交评价（一个订单只能评一次）
     */
    void submit(ReviewDTO reviewDTO);

    /**
     * 商家回复评价
     */
    void reply(ReviewReplyDTO reviewReplyDTO);

    /**
     * 评价显示/隐藏
     */
    void onOff(Integer id);

    /**
     * 删除评价
     */
    void delete(Integer id);
}
