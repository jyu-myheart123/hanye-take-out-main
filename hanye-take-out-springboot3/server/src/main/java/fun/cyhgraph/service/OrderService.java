package fun.cyhgraph.service;

import fun.cyhgraph.dto.*;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.vo.OrderPaymentVO;
import fun.cyhgraph.vo.OrderStatisticsVO;
import fun.cyhgraph.vo.OrderSubmitVO;
import fun.cyhgraph.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submit(OrderSubmitDTO orderSubmitDTO);

    /**
     * 堂食下单：员工在后台为到店客户代下单
     * 小白讲解：和微信外卖下单不同，这里由员工勾选菜品，后端统一核算价格，
     * 订单直接标记为"已付款/待接单"，进入厨房的接单流程
     */
    OrderSubmitVO dineInSubmit(DineInOrderDTO dineInOrderDTO);

    Integer unPayOrderCount();

    OrderVO getById(Integer id);

    PageResult userPage(int page, int pageSize, Integer status);

    void userCancelById(Integer id) throws Exception;

    void reOrder(Integer id);

    OrderPaymentVO payment(OrderPaymentDTO orderPaymentDTO);

    PageResult conditionSearch(OrderPageDTO orderPageDTO);

    OrderStatisticsVO statistics();

    void confirm(OrderConfirmDTO orderConfirmDTO);

    void reject(OrderRejectionDTO orderRejectionDTO);

    void cancel(OrderCancelDTO orderCancelDTO);

    void delivery(Integer id);

    void complete(Integer id);

    void reminder(Integer id);
}
