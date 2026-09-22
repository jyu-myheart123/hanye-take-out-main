package fun.cyhgraph.service.serviceImpl;

import com.alibaba.fastjson.JSON;
import fun.cyhgraph.dto.BlindBoxDTO;
import fun.cyhgraph.dto.BlindBoxDrawDTO;
import fun.cyhgraph.entity.*;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.mapper.*;
import fun.cyhgraph.service.BlindBoxService;
import fun.cyhgraph.vo.BlindBoxDrawResultVO;
import fun.cyhgraph.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 惊喜盲盒服务实现
 * 小白讲解：盲盒的价格是固定的（商家设多少就是多少），顾客付款后点一张卡牌，
 * 后端用随机数决定命运套餐，并立刻生成一张正式订单——之后接单、做菜、配送全走老流程。
 */
@Service
@Slf4j
public class BlindBoxServiceImpl implements BlindBoxService {

    @Autowired
    private BlindBoxMapper blindBoxMapper;
    @Autowired
    private BlindBoxOptionMapper blindBoxOptionMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 保存盲盒配置
     * 小白讲解：套餐选项必须恰好3个（对应三张卡牌）；编辑时先删旧选项再插新的，最简单不会乱。
     */
    @Override
    @Transactional
    public void save(BlindBoxDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new OrderBusinessException("请填写盲盒名称");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("请填写正确的盲盒价格");
        }
        List<BlindBoxDTO.OptionItem> options = dto.getOptions();
        if (options == null || options.size() != 3) {
            throw new OrderBusinessException("每个盲盒必须恰好配置3个套餐（对应三张卡牌）");
        }
        // 每个套餐至少选一道菜
        for (BlindBoxDTO.OptionItem item : options) {
            if (item.getOptionName() == null || item.getOptionName().trim().isEmpty()) {
                throw new OrderBusinessException("请填写每个套餐的名称（如：幸运套餐A）");
            }
            if (item.getDishIds() == null || item.getDishIds().trim().isEmpty()) {
                throw new OrderBusinessException("套餐【" + item.getOptionName() + "】还没有选择菜品");
            }
        }

        // 第一步：保存盲盒本身（有id=更新，无id=新增）
        BlindBox box = new BlindBox();
        BeanUtils.copyProperties(dto, box);
        box.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) {
            blindBoxMapper.insert(box);
        } else {
            blindBoxMapper.update(box);
            // 编辑前清空旧套餐
            blindBoxOptionMapper.deleteByBoxId(box.getId());
        }

        // 第二步：重新插入3个套餐选项
        for (BlindBoxDTO.OptionItem item : options) {
            BlindBoxOption option = BlindBoxOption.builder()
                    .boxId(box.getId())
                    .optionName(item.getOptionName().trim())
                    .dishIds(item.getDishIds().trim())
                    .dishSummary(item.getDishSummary())
                    .build();
            blindBoxOptionMapper.insert(option);
        }
        log.info("盲盒配置已保存：{}，售价 ¥{}", box.getName(), box.getPrice());
    }

    /**
     * 盲盒列表（每个盲盒带上自己的3个套餐）
     */
    @Override
    public List<BlindBox> listAll() {
        List<BlindBox> list = blindBoxMapper.listAll();
        for (BlindBox box : list) {
            // 把套餐列表临时挂上去：用 image 字段不合适——这里直接给 BlindBox 加个动态属性？
            // BlindBox 实体没有 options 字段，为了前端列表展示需要，使用盲盒id查询选项由前端单独调接口更清晰。
            box.setOptions(blindBoxOptionMapper.listByBoxId(box.getId()));
        }
        return list;
    }

    /**
     * 删除盲盒（同时清掉它的套餐）
     */
    @Override
    @Transactional
    public void delete(Integer id) {
        blindBoxOptionMapper.deleteByBoxId(id);
        blindBoxMapper.delete(id);
    }

    /**
     * 抽卡：随机决定套餐 + 生成正式订单
     * 小白讲解：整个过程在一个事务里——订单和明细要么一起成功要么一起失败；
     * 抽完通过 WebSocket 通知后台"来新订单啦"。
     */
    @Override
    @Transactional
    public BlindBoxDrawResultVO draw(BlindBoxDrawDTO dto) {
        // ---- 1. 校验盲盒 ----
        if (dto.getBoxId() == null) {
            throw new OrderBusinessException("请选择盲盒");
        }
        BlindBox box = blindBoxMapper.getById(dto.getBoxId());
        if (box == null) {
            throw new OrderBusinessException("盲盒不存在");
        }
        if (box.getStatus() == null || box.getStatus() != 1) {
            throw new OrderBusinessException("该盲盒已停用，不能抽取");
        }
        List<BlindBoxOption> options = blindBoxOptionMapper.listByBoxId(box.getId());
        if (options.size() != 3) {
            throw new OrderBusinessException("盲盒套餐配置不完整（需要3个套餐），请联系商家检查");
        }

        // ---- 2. 订单类型与信息校验（堂食要桌号，外卖要地址）----
        boolean isTakeout = dto.getOrderType() != null && dto.getOrderType() == 1;
        String tableNo = dto.getTableNo() == null ? "" : dto.getTableNo().trim();
        String address = dto.getAddress() == null ? "" : dto.getAddress().trim();
        if (!isTakeout && tableNo.isEmpty()) {
            throw new OrderBusinessException("请填写桌号或顾客称呼，方便后厨叫号");
        }
        if (isTakeout && address.isEmpty()) {
            throw new OrderBusinessException("外卖盲盒请填写收货地址");
        }

        // ---- 3. 随机抽卡：先随机决定"命运套餐"，再把它放到顾客点的那张卡的位置 ----
        int pickedIndex = dto.getPickedIndex() == null ? 0 : dto.getPickedIndex();
        if (pickedIndex < 0 || pickedIndex > 2) {
            pickedIndex = 0;
        }
        int randomPick = new Random().nextInt(3);
        BlindBoxOption won = options.get(randomPick);
        // 组装展示顺序：被点位置放命运套餐，其余两个套餐随机放到另外两个位置
        List<BlindBoxOption> others = new ArrayList<>(options);
        others.remove(randomPick);
        Collections.shuffle(others);
        List<BlindBoxOption> display = new ArrayList<>(3);
        int otherCursor = 0;
        for (int i = 0; i < 3; i++) {
            display.add(i == pickedIndex ? won : others.get(otherCursor++));
        }
        log.info("盲盒抽卡：盲盒={}，顾客点第{}张，命运套餐 → {}", box.getName(), pickedIndex + 1, won.getOptionName());

        // ---- 4. 解析三张卡的菜品（命运卡的菜品要进订单和明细）----
        List<BlindBoxDrawResultVO.DrawnCard> cards = new ArrayList<>();
        List<OrderDetail> wonDetails = new ArrayList<>();
        BigDecimal originalAmount = BigDecimal.ZERO;
        for (int i = 0; i < display.size(); i++) {
            BlindBoxOption op = display.get(i);
            BlindBoxDrawResultVO.DrawnCard card = new BlindBoxDrawResultVO.DrawnCard();
            card.setOptionId(op.getId());
            card.setOptionName(op.getOptionName());
            card.setDishSummary(op.getDishSummary());

            List<BlindBoxDrawResultVO.CardDish> cardDishes = new ArrayList<>();
            for (String idStr : op.getDishIds().split(",")) {
                Integer dishId = Integer.valueOf(idStr.trim());
                Dish dish = dishMapper.getById(dishId);
                if (dish == null) {
                    throw new OrderBusinessException("套餐里的某道菜已不存在，请重新配置盲盒");
                }
                BlindBoxDrawResultVO.CardDish cd = new BlindBoxDrawResultVO.CardDish();
                cd.setDishId(dish.getId());
                cd.setName(dish.getName());
                cd.setPic(dish.getPic());
                cd.setPrice(dish.getPrice());
                cd.setNumber(1);
                cardDishes.add(cd);

                // 只有命运套餐才计入订单金额与明细
                if (i == pickedIndex) {
                    originalAmount = originalAmount.add(dish.getPrice());
                    wonDetails.add(OrderDetail.builder()
                            .name(dish.getName())
                            .pic(dish.getPic())
                            .dishId(dish.getId())
                            .number(1)
                            .amount(dish.getPrice())
                            .build());
                }
            }
            card.setDishes(cardDishes);
            cards.add(card);
        }

        // 盲盒优惠 = 菜品原价 − 盲盒价（不会让它是负数）
        BigDecimal discountAmount = originalAmount.subtract(box.getPrice());
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        // ---- 5. 生成正式订单（和堂食单结构保持一致，后续流程直接复用）----
        LocalDateTime now = LocalDateTime.now();
        String contactPhone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        Order order = Order.builder()
                .number(String.valueOf(System.currentTimeMillis()))
                .status(Order.TO_BE_CONFIRMED) // 已付款 → 待接单
                .userId(0)
                .addressBookId(0)
                .orderTime(now)
                .checkoutTime(now)
                .payMethod(3) // 3=线下现金/扫码收款
                .payStatus(Order.PAID) // 顾客已先付款
                .amount(box.getPrice())
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .memberDiscount(BigDecimal.ZERO)
                .pointsDeduction(BigDecimal.ZERO)
                .blindBoxId(box.getId())
                .blindBoxOptionId(won.getId())
                .remark(dto.getRemark())
                .phone(contactPhone.isEmpty() ? null : contactPhone)
                .address(isTakeout ? address : "堂食")
                .consignee(isTakeout
                        ? (dto.getCustomerName() == null || dto.getCustomerName().trim().isEmpty()
                                ? "外卖顾客" : dto.getCustomerName().trim())
                        : tableNo)
                .deliveryStatus(1)
                .packAmount(0)
                .tablewareStatus(1)
                .orderType(isTakeout ? 1 : 2)
                .build();
        orderMapper.insert(order);

        // 明细关联订单id后批量入库
        for (OrderDetail detail : wonDetails) {
            detail.setOrderId(order.getId());
        }
        orderDetailMapper.insertBatch(wonDetails);

        // ---- 6. WebSocket 推送来单提醒（和堂食/外卖单同款消息）----
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", 1);
        msg.put("orderId", order.getId());
        msg.put("content", "惊喜盲盒订单号：" + order.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(msg));
        log.info("盲盒订单生成成功：订单id={}，实付 ¥{}，原价 ¥{}", order.getId(), box.getPrice(), originalAmount);

        // ---- 7. 组装翻牌结果返回 ----
        BlindBoxDrawResultVO result = new BlindBoxDrawResultVO();
        result.setOrderId(order.getId());
        result.setOrderNumber(order.getNumber());
        result.setOrderType(order.getOrderType());
        result.setWonIndex(pickedIndex);
        result.setCards(cards);
        result.setBoxPrice(box.getPrice());
        result.setOriginalAmount(originalAmount);
        result.setDiscountAmount(discountAmount);
        return result;
    }
}
