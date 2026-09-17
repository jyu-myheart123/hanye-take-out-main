package fun.cyhgraph.service.serviceImpl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.constant.MessageConstant;
import fun.cyhgraph.context.BaseContext;
import fun.cyhgraph.dto.*;
import fun.cyhgraph.entity.*;
import fun.cyhgraph.exception.AddressBookBusinessException;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.exception.ShoppingCartBusinessException;
import fun.cyhgraph.mapper.*;
import fun.cyhgraph.promotion.CalcItem;
import fun.cyhgraph.promotion.PromotionEngine;
import fun.cyhgraph.promotion.PromotionResult;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.MemberService;
import fun.cyhgraph.service.OrderService;
import fun.cyhgraph.utils.HttpClientUtil;
import fun.cyhgraph.utils.WeChatPayUtil;
import fun.cyhgraph.vo.DineInPriceVO;
import fun.cyhgraph.vo.MemberConsumeResult;
import fun.cyhgraph.vo.MemberVO;
import fun.cyhgraph.vo.OrderPaymentVO;
import fun.cyhgraph.vo.OrderStatisticsVO;
import fun.cyhgraph.vo.OrderSubmitVO;
import fun.cyhgraph.vo.OrderVO;
import fun.cyhgraph.websocket.WebSocketServer;
import fun.cyhgraph.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    private Order order;
    @Autowired
    private WebSocketServer webSocketServer;
    // 营销活动引擎：开单时自动挑最优惠的活动
    @Autowired
    private PromotionEngine promotionEngine;
    // 会员服务：识别会员、等级折扣、积分抵扣、余额支付、消费后自动升级
    @Autowired
    private MemberService memberService;
    // 这个Value是annotation注解的包，不是lombok的！
    @Value("${hanye.shop.address}")
    private String shopAddress;
    @Value("${hanye.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     *
     * @param orderSubmitDTO
     * @return
     */
    public OrderSubmitVO submit(OrderSubmitDTO orderSubmitDTO) {
        // 1、查询校验地址情况
        AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 不能超出配送范围
        // checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        // 2、查询校验购物车情况
        Integer userId = BaseContext.getCurrentId();
        Cart cart = new Cart();
        cart.setUserId(userId);
        List<Cart> cartList = cartMapper.list(cart);
        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.CART_IS_NULL);
        }
        // 3、构建订单数据
        Order order = new Order();
        BeanUtils.copyProperties(orderSubmitDTO, order);
        order.setAddressBookId(orderSubmitDTO.getAddressId());
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        // 利用时间戳来生成当前订单的编号
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Order.PENDING_PAYMENT); // 刚下单提交，此时是待付款状态
        order.setPayStatus(Order.UN_PAID); // 未支付
        order.setOrderTime(LocalDateTime.now());
        this.order = order;
        // 4、向订单表插入1条数据
        orderMapper.insert(order);
        // 订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 遍历购物车中所有的商品，逐个加到订单明细表
        for (Cart c : cartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(c, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        // 5、向明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);
        // 6、清理购物车中的数据
        cartMapper.delete(userId);
        // 7、封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 堂食下单（员工在后台为到店客户代下单）
     * 小白讲解流程：
     *   1. 员工在前端勾选菜品/套餐 -> 只传 id 和数量过来，不传价格（防止篡改）
     *   2. 后端根据 id 查数据库真实单价，算出原价合计
     *   3. 营销引擎自动挑一个"省得最多"的活动（满减/折扣/第二份半价/买一送一）
     *   4. 识别会员手机号：会员等级在活动价上再打折，积分还能抵钱，余额可直接支付
     *   5. 生成订单+明细，会员扣余额/加积分/累计消费/自动升级全部在同一个事务里
     *   6. 堂食现场已付款，订单直接是"待接单"状态，后厨立刻能看到
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public DineInPriceVO dineInSubmit(DineInOrderDTO dineInOrderDTO) {
        List<DineInOrderDTO.Item> items = dineInOrderDTO.getItems();
        if (items == null || items.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.DINE_IN_EMPTY);
        }
        if (dineInOrderDTO.getTableNo() == null || dineInOrderDTO.getTableNo().trim().isEmpty()) {
            throw new OrderBusinessException("请填写桌号或客户称呼，方便后厨叫号");
        }
        // 联系电话是选填的，但只要填了就必须是11位手机号（防止多输一位导致订单入库失败、整单回滚）
        String contactPhoneInput = dineInOrderDTO.getPhone() == null ? "" : dineInOrderDTO.getPhone().trim();
        if (!contactPhoneInput.isEmpty() && !contactPhoneInput.matches("^1\\d{10}$")) {
            throw new OrderBusinessException("联系电话格式不正确，请填写11位手机号，或留空不填");
        }

        // 第一步：后端重新核算价格（活动优惠、会员折扣、积分抵扣），不信前端任何金额
        DineInPriceVO priceVO = calculatePrice(dineInOrderDTO);

        // 第二步：会员处理。填了会员手机号就识别/自动建档；选了余额支付必须是会员
        boolean payByBalance = dineInOrderDTO.getPayMethod() != null && dineInOrderDTO.getPayMethod() == 2;
        String memberPhone = normalizePhone(dineInOrderDTO.getMemberPhone());
        Member member = null;
        if (memberPhone != null) {
            member = memberService.registerIfAbsent(memberPhone);
        }
        if (payByBalance && member == null) {
            throw new OrderBusinessException("使用余额支付前，请先填写会员手机号识别会员");
        }

        // 第三步：构建堂食订单（原价、各项优惠、实付都落库，订单详情里能对账）
        LocalDateTime now = LocalDateTime.now();
        String contactPhone = dineInOrderDTO.getPhone() != null && !dineInOrderDTO.getPhone().trim().isEmpty()
                ? dineInOrderDTO.getPhone().trim() : memberPhone;
        Order order = Order.builder()
                .number(String.valueOf(System.currentTimeMillis())) // 用时间戳当订单号
                .status(Order.TO_BE_CONFIRMED)   // 堂食现场已付款，直接进入"待接单"
                .userId(0)                        // 堂食客户不是微信用户，用 0 占位
                .addressBookId(0)                 // 堂食没有收货地址，用 0 占位
                .orderTime(now)
                .checkoutTime(now)
                .payMethod(payByBalance ? 4 : 3)  // 3=线下现金/扫码，4=会员余额
                .payStatus(Order.PAID)            // 现场已付款
                .amount(priceVO.getPayAmount())
                .originalAmount(priceVO.getOriginalAmount())
                .discountAmount(priceVO.getDiscountAmount())
                .memberDiscount(priceVO.getMemberDiscount())
                .pointsDeduction(priceVO.getPointsDeduction())
                .promotionId(priceVO.getPromotionId())
                .promotionName(priceVO.getPromotionName())
                .memberId(member == null ? null : member.getId())
                .remark(dineInOrderDTO.getRemark())
                .phone(contactPhone)
                .address("堂食")
                .consignee(dineInOrderDTO.getTableNo().trim()) // 收货人位置存"桌号/称呼"
                .deliveryStatus(1)
                .packAmount(0)
                .tablewareStatus(1)
                .orderType(2)                     // 2=堂食（1=外卖）
                .build();
        orderMapper.insert(order);

        // 订单明细关联订单id，批量插入（明细金额仍是原价小计，优惠在订单层体现）
        for (OrderDetail detail : priceVO.getDetails() == null ? new ArrayList<OrderDetail>() : priceVO.getDetails()) {
            detail.setOrderId(order.getId());
        }
        orderDetailMapper.insertBatch(priceVO.getDetails());

        // 第四步：会员结算（扣余额/积分抵扣/返积分/累计消费/自动升级/写流水），和订单同事务
        MemberConsumeResult consumeResult = null;
        if (member != null) {
            consumeResult = memberService.consume(order.getId(), memberPhone,
                    priceVO.getPayAmount(), priceVO.getPointsDeduction(), payByBalance);
            priceVO.setMemberId(member.getId());
            priceVO.setMemberLevel(consumeResult.getLevel());
            priceVO.setMemberLevelName(consumeResult.getLevelName());
            priceVO.setBalanceAfter(consumeResult.getBalanceAfter());
            priceVO.setPointsAfter(consumeResult.getPointsAfter());
            priceVO.setPointsEarned(consumeResult.getPointsEarned());
        }

        // 第五步：WebSocket 给后台推送来单提醒
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", order.getId());
        map.put("content", "堂食订单号：" + order.getNumber());
        String json = JSON.toJSONString(map);
        log.info("堂食开单成功，推送给后台：{}，实付 ¥{}", map, order.getAmount());
        webSocketServer.sendToAllClient(json);

        // 回填订单信息后返回
        priceVO.setId(order.getId());
        priceVO.setOrderNumber(order.getNumber());
        priceVO.setOrderTime(order.getOrderTime());
        priceVO.setOrderAmount(order.getAmount());
        return priceVO;
    }

    /**
     * 堂食开单价格试算（只算账，不生成订单）
     */
    @Override
    public DineInPriceVO preview(DineInOrderDTO dineInOrderDTO) {
        if (dineInOrderDTO.getItems() == null || dineInOrderDTO.getItems().isEmpty()) {
            // 空清单直接返回一个全0的空账，前端友好展示
            return emptyPriceVO();
        }
        return calculatePrice(dineInOrderDTO);
    }

    /**
     * 堂食价格核算核心方法（试算和正式下单共用，保证"看到的价"和"实收的价"完全一致）
     */
    private DineInPriceVO calculatePrice(DineInOrderDTO dineInOrderDTO) {
        // 1. 逐项查库核价：价格一律以后端数据库为准
        List<OrderDetail> details = new ArrayList<>();
        List<CalcItem> calcItems = new ArrayList<>();
        BigDecimal originalAmount = BigDecimal.ZERO;
        for (DineInOrderDTO.Item item : dineInOrderDTO.getItems()) {
            int number = (item.getNumber() == null || item.getNumber() < 1) ? 1 : item.getNumber();
            BigDecimal price;
            String name;
            String pic;
            Integer categoryId;
            if (item.getDishId() != null) {
                Dish dish = dishMapper.getById(item.getDishId());
                if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
                    throw new OrderBusinessException(MessageConstant.DISH_NOT_AVAILABLE);
                }
                price = dish.getPrice();
                name = dish.getName();
                pic = dish.getPic();
                categoryId = dish.getCategoryId();
            } else if (item.getSetmealId() != null) {
                Setmeal setmeal = setmealMapper.getSetmealById(item.getSetmealId());
                if (setmeal == null || setmeal.getStatus() == null || setmeal.getStatus() != 1) {
                    throw new OrderBusinessException(MessageConstant.DISH_NOT_AVAILABLE);
                }
                price = setmeal.getPrice();
                name = setmeal.getName();
                pic = setmeal.getPic();
                categoryId = setmeal.getCategoryId();
            } else {
                throw new OrderBusinessException(MessageConstant.DISH_NOT_AVAILABLE);
            }
            BigDecimal itemAmount = price.multiply(BigDecimal.valueOf(number));
            originalAmount = originalAmount.add(itemAmount);

            details.add(OrderDetail.builder()
                    .name(name)
                    .pic(pic)
                    .dishId(item.getDishId())
                    .setmealId(item.getSetmealId())
                    .dishFlavor(item.getDishFlavor())
                    .number(number)
                    .amount(itemAmount)
                    .build());
            calcItems.add(CalcItem.builder()
                    .dishId(item.getDishId())
                    .setmealId(item.getSetmealId())
                    .categoryId(categoryId)
                    .name(name)
                    .unitPrice(price)
                    .number(number)
                    .build());
        }

        // 2. 营销引擎：自动选出本单最优惠的活动
        PromotionResult bestPromotion = promotionEngine.calculateBest(calcItems);
        BigDecimal discountAmount = bestPromotion == null ? BigDecimal.ZERO : bestPromotion.getSaving();

        // 3. 会员识别 + 等级折扣（在活动优惠后的金额上再打折）+ 积分抵扣
        BigDecimal memberDiscount = BigDecimal.ZERO;
        BigDecimal pointsDeduction = BigDecimal.ZERO;
        BigDecimal maxPointsDeduction = BigDecimal.ZERO;
        MemberVO member = memberService.recognizeByPhone(dineInOrderDTO.getMemberPhone());
        if (member != null) {
            BigDecimal afterPromotion = originalAmount.subtract(discountAmount);
            memberDiscount = memberService.calcLevelDiscount(member, afterPromotion);
            BigDecimal afterMember = afterPromotion.subtract(memberDiscount);
            boolean usePoints = Boolean.TRUE.equals(dineInOrderDTO.getUsePoints());
            pointsDeduction = memberService.calcPointsDeduction(member, afterMember, usePoints);
            // 最多能抵多少（勾选积分框时前端要提示）
            maxPointsDeduction = memberService.calcPointsDeduction(member, afterMember, true);
        }

        // 4. 实付金额 = 原价 - 活动优惠 - 会员折扣 - 积分抵扣
        BigDecimal payAmount = originalAmount
                .subtract(discountAmount)
                .subtract(memberDiscount)
                .subtract(pointsDeduction)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // 5. 组装试算结果
        DineInPriceVO.DineInPriceVOBuilder builder = DineInPriceVO.builder()
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .memberDiscount(memberDiscount)
                .pointsDeduction(pointsDeduction)
                .maxPointsDeduction(maxPointsDeduction)
                .payAmount(payAmount)
                .orderAmount(payAmount)
                .tips(bestPromotion == null ? new ArrayList<>() : bestPromotion.getTips());
        if (bestPromotion != null) {
            builder.promotionId(bestPromotion.getPromotionId())
                    .promotionName(bestPromotion.getName())
                    .promotionType(bestPromotion.getType());
        }
        if (member != null) {
            String displayName = member.getName() != null && !member.getName().isEmpty()
                    ? member.getName() : "会员" + member.getPhone().substring(7);
            builder.memberId(member.getId())
                    .memberName(displayName)
                    .memberPhone(member.getPhone())
                    .memberLevel(member.getLevel())
                    .memberLevelName(member.getLevelName())
                    .levelDiscount(member.getDiscount())
                    .balance(member.getBalance())
                    .points(member.getPoints());
        }
        DineInPriceVO priceVO = builder.build();
        // 明细只在内部提交时使用，不序列化给试算响应也无妨（多带一个字段无所谓）
        priceVO.setDetails(details);
        return priceVO;
    }

    /**
     * 空清单的试算结果（页面刚打开、还没点菜时用）
     */
    private DineInPriceVO emptyPriceVO() {
        return DineInPriceVO.builder()
                .originalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .memberDiscount(BigDecimal.ZERO)
                .pointsDeduction(BigDecimal.ZERO)
                .maxPointsDeduction(BigDecimal.ZERO)
                .payAmount(BigDecimal.ZERO)
                .orderAmount(BigDecimal.ZERO)
                .tips(new ArrayList<>())
                .build();
    }

    /**
     * 会员手机号简单规范化：去空格，不是11位手机号返回null
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.matches("^1\\d{10}$") ? trimmed : null;
    }

    /**
     * 当前用户未支付订单数量
     *
     * @return
     */
    public Integer unPayOrderCount() {
        Integer userId = BaseContext.getCurrentId();
        return orderMapper.getUnPayCount(userId);
    }

    /**
     * 根据id查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO getById(Integer id) {
        Order order = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 用户端条件分页查询历史订单
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult userPage(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        // 要根据当前用户和状态条件来查询订单，因此设计OrderPageDTO来封装信息
        OrderPageDTO orderPageDTO = new OrderPageDTO();
        orderPageDTO.setUserId(BaseContext.getCurrentId());
        orderPageDTO.setStatus(status);
        Page<Order> orderPage = orderMapper.page(orderPageDTO);
        // 查到所有订单orderPage后，封装成OrderVO列表返回
        List<OrderVO> list = new ArrayList<>();
        // 其实就是将每个订单都加上订单详情OrderDetail
        if (orderPage != null && orderPage.getTotal() > 0) {
            for (Order order : orderPage) {
                Integer orderId = order.getId(); // 订单id
                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getById(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(orderPage.getTotal(), list);
    }

    /**
     * 用户根据订单id取消订单
     *
     * @param id
     */
    public void userCancelById(Integer id) throws Exception {
        // 根据id查询订单
        Order ordersDB = orderMapper.getById(id);
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消  前两个状态才能直接退款，否则要联系商家
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Order order = new Order();
        order.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            // 调用微信支付退款接口（refund有异常需要抛出处理）
            // 用不了
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            // 支付状态修改为 退款
            order.setPayStatus(Order.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        order.setStatus(Order.CANCELLED);
        order.setCancelReason("用户取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 根据订单id再来一单
     *
     * @param id
     */
    public void reOrder(Integer id) {
        Integer userId = BaseContext.getCurrentId();
        // 1、先拿到这个订单id的所有菜品
        List<OrderDetail> detailList = orderDetailMapper.getById(id);
        // 2、将订单详情对象转换为购物车对象
//        List<Cart> cartList = new ArrayList<>();
//        for (OrderDetail orderDetail : detailList){
//            Cart cart = new Cart();
//            BeanUtils.copyProperties(orderDetail, cart, "id");
//            cart.setUserId(userId);
//            cart.setCreateTime(LocalDateTime.now());
//            cartList.add(cart);
//        }
        List<Cart> cartList = detailList.stream().map(x -> {
            Cart cart = new Cart();
            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, cart, "id");
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            return cart;
        }).toList();
        // 3、将购物车对象批量添加到数据库
        cartMapper.insertBatch(cartList);
    }

    /**
     * 用户支付订单
     *
     * @param orderPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrderPaymentDTO orderPaymentDTO) {
        // 当前登录用户id
        Integer userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        // 调用微信支付接口，生成预支付交易单
        // 暂时不做，而是把 weChatUtils 里相关的参数设置好，让后续代码不出问题
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        // 抽取 paySuccess 的代码：不搞支付，修改订单状态后直接更新数据库，并返回给前端
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Order.PAID; // 支付状态，已支付
        Integer OrderStatus = Order.TO_BE_CONFIRMED;  // 订单状态，待接单
        LocalDateTime checkOutTime = LocalDateTime.now(); // 更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, checkOutTime, this.order.getId());

        // 由于跳过了微信支付，因此没有通过微信来调用paySuccess方法，所以把里面的消息提醒方法抽出来放到这里！
        // 通过websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type", 1); // 消息类型，1表示来单提醒（2表示客户催单）
        map.put("orderId", this.order.getId());
        map.put("content", "订单号：" + this.order.getNumber());
        String json = JSON.toJSONString(map);
        log.info("发给商家端啊！：{}", map);
        webSocketServer.sendToAllClient(json);

        return vo;
    }

    /**
     * 条件分页查询订单信息
     *
     * @param orderPageDTO
     * @return
     */
    public PageResult conditionSearch(OrderPageDTO orderPageDTO) {
        PageHelper.startPage(orderPageDTO.getPage(), orderPageDTO.getPageSize());
        Page<Order> orders = orderMapper.page(orderPageDTO);
        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(orders);
        return new PageResult(orders.getTotal(), orderVOList);
    }

    /**
     * 不同状态订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、已接单/待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countByStatus(Order.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Order.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Order.DELIVERY_IN_PROGRESS);
        // 封装成VO返回
        return OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    /**
     * 接单
     *
     * @param orderConfirmDTO
     */
    public void confirm(OrderConfirmDTO orderConfirmDTO) {
        Order order = Order.builder()
                .id(orderConfirmDTO.getId())
                .status(Order.CONFIRMED)
                .build();
        orderMapper.update(order);
    }

    /**
     * 拒单
     *
     * @param orderRejectionDTO
     */
    public void reject(OrderRejectionDTO orderRejectionDTO) {
        Integer orderId = orderRejectionDTO.getId();
        Order orderDB = orderMapper.getById(orderId);
        Order order = new Order();
        // 订单只有存在且状态为2（待接单）才可以拒单
        if (orderDB == null || !orderDB.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);

        }
        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        order.setId(orderDB.getId());
        // 调用微信支付退款接口，但关于微信的接口都用不了，支付状态修改为 退款 就行
        order.setPayStatus(Order.REFUND);
        order.setStatus(Order.CANCELLED);
        order.setRejectionReason(orderRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 取消订单
     *
     * @param orderCancelDTO
     */
    public void cancel(OrderCancelDTO orderCancelDTO) {
        Integer orderId = orderCancelDTO.getId();
        Order orderDB = orderMapper.getById(orderId);
        Order order = new Order();
        // 取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        order.setId(orderDB.getId());
        // 调用微信支付退款接口，但关于微信的接口都用不了，支付状态修改为 退款 就行
        order.setPayStatus(Order.REFUND);
        order.setStatus(Order.CANCELLED);
        order.setCancelReason(orderCancelDTO.getCancelReason());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 根据id派送订单
     *
     * @param id
     */
    public void delivery(Integer id) {
        Order orderDB = orderMapper.getById(id);
        // 订单存在 且 状态为3已接单，才能进行派送操作
        if (orderDB == null || !orderDB.getStatus().equals(Order.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Order order = new Order();
        order.setId(orderDB.getId());
        order.setStatus(Order.DELIVERY_IN_PROGRESS);
        orderMapper.update(order);
    }

    /**
     * 完成订单
     * 小白讲解：外卖单必须先"派送中(4)"才能完成；堂食单没有配送环节，
     * 后厨出餐、顾客就餐结束后在"已接单(3)"状态就可以直接完成，完成后顾客就能评价。
     *
     * @param id
     */
    public void complete(Integer id) {
        Order orderDB = orderMapper.getById(id);
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        boolean takeoutCanComplete = Order.DELIVERY_IN_PROGRESS.equals(orderDB.getStatus());
        boolean dineInCanComplete = Integer.valueOf(2).equals(orderDB.getOrderType())
                && Order.CONFIRMED.equals(orderDB.getStatus());
        if (!takeoutCanComplete && !dineInCanComplete) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Order order = new Order();
        order.setId(orderDB.getId());
        order.setStatus(Order.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now()); // 堂食单复用该字段记录完成时间
        orderMapper.update(order);
    }

    /**
     * 用户催单
     * @param id
     */
    public void reminder(Integer id) {
        Order orderDB = orderMapper.getById(id);
        // 订单不存在
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 通过websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type", 2); // 消息类型，2表示客户催单（1表示来单提醒）
        map.put("orderId", id);
        map.put("content", "订单号：" + orderDB.getNumber());
        String json = JSON.toJSONString(map);
        log.info("发给商家端啊！：{}", map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 抽出page.getResult()的内容，其中的订单菜品需要有详情信息
     *
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVOList(Page<Order> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Order> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Order orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param order
     * @return
     */
    private String getOrderDishesStr(Order order) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getById(order.getId());
        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3;）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     *
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        // 获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        // 数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        // 店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address", address);

        // 获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        // 数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        // 用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        // 路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        // 数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

}
