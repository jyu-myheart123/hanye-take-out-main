package fun.cyhgraph.service.serviceImpl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.dto.ReviewDTO;
import fun.cyhgraph.dto.ReviewReplyDTO;
import fun.cyhgraph.entity.Order;
import fun.cyhgraph.entity.OrderDetail;
import fun.cyhgraph.entity.Review;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.mapper.OrderDetailMapper;
import fun.cyhgraph.mapper.OrderMapper;
import fun.cyhgraph.mapper.ReviewMapper;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.ReviewService;
import fun.cyhgraph.vo.OrderVO;
import fun.cyhgraph.vo.ReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户评价服务实现
 * 小白讲解：只有"已完成的堂食单"才能评价，而且一单一评；
 * 评价列表会带上这单点了哪些菜，后厨看反馈时知道说的是哪道菜。
 */
@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 待评价的已完成堂食单分页
     */
    @Override
    public PageResult pagePending(int page, int pageSize, String phone) {
        PageHelper.startPage(page, pageSize);
        String keyword = phone == null || phone.trim().isEmpty() ? null : phone.trim();
        Page<Order> orderPage = reviewMapper.pagePendingReview(keyword);
        // 每个订单带上菜品明细，评价表单里展示给顾客核对
        List<OrderVO> list = new ArrayList<>();
        for (Order order : orderPage) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setOrderDetailList(orderDetailMapper.getById(order.getId()));
            vo.setOrderDishes(buildDishesStr(vo.getOrderDetailList()));
            list.add(vo);
        }
        return new PageResult(orderPage.getTotal(), list);
    }

    /**
     * 评价列表分页
     */
    @Override
    public PageResult pageReviews(int page, int pageSize, String keyword, Integer status) {
        PageHelper.startPage(page, pageSize);
        String kw = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
        Page<ReviewVO> reviewPage = reviewMapper.pageQuery(kw, status);
        for (ReviewVO vo : reviewPage) {
            List<OrderDetail> details = orderDetailMapper.getById(vo.getOrderId());
            vo.setOrderDetailList(details);
            vo.setOrderDishes(buildDishesStr(details));
        }
        return new PageResult(reviewPage.getTotal(), reviewPage.getResult());
    }

    /**
     * 提交评价
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(ReviewDTO dto) {
        if (dto.getOrderId() == null) {
            throw new OrderBusinessException("请选择要评价的订单");
        }
        Order order = orderMapper.getById(dto.getOrderId());
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }
        // 只有已完成的堂食单才能评价
        if (!Order.COMPLETED.equals(order.getStatus())) {
            throw new OrderBusinessException("只有已完成的订单才能评价");
        }
        if (!Integer.valueOf(2).equals(order.getOrderType())) {
            throw new OrderBusinessException("只有堂食订单支持客户评价");
        }
        if (reviewMapper.getByOrderId(order.getId()) != null) {
            throw new OrderBusinessException("该订单已经评价过了");
        }
        if (dto.getDishScore() == null || dto.getServiceScore() == null) {
            throw new OrderBusinessException("请为菜品和服务态度打分");
        }
        if (!validScore(dto.getDishScore()) || !validScore(dto.getServiceScore())
                || (dto.getTasteScore() != null && !validScore(dto.getTasteScore()))
                || (dto.getSpeedScore() != null && !validScore(dto.getSpeedScore()))) {
            throw new OrderBusinessException("评分必须在1~5星之间");
        }

        Review review = Review.builder()
                .orderId(order.getId())
                .orderNumber(order.getNumber())
                .tableNo(order.getConsignee())
                .memberId(order.getMemberId())
                .customerName(trimToNull(dto.getCustomerName()))
                .dishScore(dto.getDishScore())
                .tasteScore(dto.getTasteScore())
                .serviceScore(dto.getServiceScore())
                .speedScore(dto.getSpeedScore())
                .content(trimToNull(dto.getContent()))
                .suggestion(trimToNull(dto.getSuggestion()))
                .status(1)
                .build();
        reviewMapper.insert(review);
        log.info("堂食订单[{}]提交评价：菜品{}星 服务{}星", order.getNumber(),
                dto.getDishScore(), dto.getServiceScore());
    }

    /**
     * 商家回复
     */
    @Override
    public void reply(ReviewReplyDTO dto) {
        if (dto.getId() == null || reviewMapper.getById(dto.getId()) == null) {
            throw new OrderBusinessException("评价不存在");
        }
        if (dto.getReply() == null || dto.getReply().trim().isEmpty()) {
            throw new OrderBusinessException("回复内容不能为空");
        }
        reviewMapper.updateReply(dto.getId(), dto.getReply().trim());
    }

    /**
     * 显示/隐藏
     */
    @Override
    public void onOff(Integer id) {
        reviewMapper.onOff(id);
    }

    /**
     * 删除
     */
    @Override
    public void delete(Integer id) {
        reviewMapper.delete(id);
    }

    /**
     * 校验星级范围1-5
     */
    private boolean validScore(Integer score) {
        return score >= 1 && score <= 5;
    }

    /**
     * 空字符串转null，方便入库
     */
    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 把订单菜品拼成"宫保鸡丁*2;米饭*1;"的摘要
     */
    private String buildDishesStr(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        return details.stream()
                .map(d -> d.getName() + "*" + d.getNumber() + ";")
                .collect(Collectors.joining(""));
    }
}
