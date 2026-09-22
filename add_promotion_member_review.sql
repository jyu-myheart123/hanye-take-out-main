-- ============================================================
-- 功能五/六/客户评价：营销活动引擎 + 堂食会员体系 + 客户评价
-- 数据库迁移脚本（在 hanye_take_out 库执行）
-- 小白讲解：把这段 SQL 整个复制到 Navicat / MySQL 命令行执行一次即可，
--          已存在的表/数据不会被破坏（IF NOT EXISTS、INSERT IGNORE）。
-- ============================================================

-- 1. 营销活动表（满减 / 折扣 / 第二份半价 / 买一送一）
CREATE TABLE IF NOT EXISTS promotion (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '活动名称',
    type        TINYINT      NOT NULL COMMENT '类型 1满减 2折扣 3第二份半价 4买一送一',
    rule_json   VARCHAR(500) NOT NULL DEFAULT '{}' COMMENT '规则JSON：满减=多档门槛/减免，折扣=折数',
    scope_type  TINYINT      NOT NULL DEFAULT 0 COMMENT '适用范围 0全场 1指定分类 2指定菜品',
    scope_ids   VARCHAR(255) DEFAULT NULL COMMENT '适用的分类id/菜品id，逗号分隔',
    begin_time  DATETIME     NOT NULL COMMENT '活动开始时间',
    end_time    DATETIME     NOT NULL COMMENT '活动结束时间',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '活动说明',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动表';

-- 2. 堂食会员表（手机号会员 + 储值 + 积分 + 等级）
CREATE TABLE IF NOT EXISTS member (
    id             INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(32)  DEFAULT NULL COMMENT '会员姓名（选填）',
    phone          VARCHAR(11)  NOT NULL COMMENT '手机号',
    balance        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '储值余额',
    points         INT          NOT NULL DEFAULT 0 COMMENT '当前积分',
    total_recharge DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值本金',
    total_gift     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计赠送金额',
    total_consume  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费（决定会员等级）',
    level          TINYINT      NOT NULL DEFAULT 1 COMMENT '等级 1银卡 2金卡 3钻石',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='堂食会员表';

-- 3. 会员余额/积分变动流水表（充值、消费、退款、积分累计/抵扣）
CREATE TABLE IF NOT EXISTS member_flow (
    id            INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id     INT          NOT NULL COMMENT '会员id',
    type          TINYINT      NOT NULL COMMENT '类型 1充值 2消费 3退款 4积分累计 5积分抵扣 6人工调整',
    amount        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额变动（正=入账，负=扣减）',
    points        INT          NOT NULL DEFAULT 0 COMMENT '积分变动（正=增加，负=减少）',
    balance_after DECIMAL(10,2) DEFAULT NULL COMMENT '变动后余额',
    points_after  INT          DEFAULT NULL COMMENT '变动后积分',
    order_id      INT          DEFAULT NULL COMMENT '关联订单id',
    remark        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_flow_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员余额/积分流水表';

-- 4. 会员等级规则表（累计消费门槛 + 等级折扣）
CREATE TABLE IF NOT EXISTS member_level_rule (
    id               INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    level            TINYINT      NOT NULL COMMENT '等级 1银卡 2金卡 3钻石',
    level_name       VARCHAR(16)  NOT NULL COMMENT '等级名称',
    threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '升级所需累计消费',
    discount         DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '等级折扣 1=不打折 0.95=95折',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级规则表';

-- 默认等级：银卡0元不打折，金卡满500享95折，钻石满2000享9折（后台可改）
INSERT IGNORE INTO member_level_rule (level, level_name, threshold_amount, discount)
VALUES (1, '银卡会员', 0.00, 1.00),
       (2, '金卡会员', 500.00, 0.95),
       (3, '钻石会员', 2000.00, 0.90);

-- 5. 客户评价表（已完成堂食订单：菜品/口味/服务/出餐评分 + 好评 + 改进意见 + 商家回复）
CREATE TABLE IF NOT EXISTS review (
    id            INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      INT          NOT NULL COMMENT '订单id（一个订单只能评价一次）',
    order_number  VARCHAR(50)  DEFAULT NULL COMMENT '订单号',
    table_no      VARCHAR(32)  DEFAULT NULL COMMENT '桌号/称呼',
    member_id     INT          DEFAULT NULL COMMENT '会员id',
    customer_name VARCHAR(32)  DEFAULT NULL COMMENT '评价人称呼',
    dish_score    TINYINT      NOT NULL COMMENT '菜品评分1-5',
    taste_score   TINYINT      DEFAULT NULL COMMENT '口味评分1-5',
    service_score TINYINT      NOT NULL COMMENT '服务态度评分1-5',
    speed_score   TINYINT      DEFAULT NULL COMMENT '出餐速度评分1-5',
    content       VARCHAR(500) DEFAULT NULL COMMENT '好评内容',
    suggestion    VARCHAR(500) DEFAULT NULL COMMENT '改进意见',
    reply         VARCHAR(500) DEFAULT NULL COMMENT '商家回复',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1展示 0隐藏',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户评价表';

-- 6. orders 表扩展：优惠明细 + 会员关联（老订单默认 0/NULL，不影响历史数据）
ALTER TABLE orders
    ADD COLUMN original_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠前原价'      AFTER amount,
    ADD COLUMN discount_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '营销活动优惠金额' AFTER original_amount,
    ADD COLUMN member_discount  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '会员等级优惠金额' AFTER discount_amount,
    ADD COLUMN points_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '积分抵扣金额'    AFTER member_discount,
    ADD COLUMN promotion_id     INT           DEFAULT NULL COMMENT '命中的营销活动id'        AFTER points_deduction,
    ADD COLUMN promotion_name   VARCHAR(64)   DEFAULT NULL COMMENT '命中的营销活动名称'      AFTER promotion_id,
    ADD COLUMN member_id        INT           DEFAULT NULL COMMENT '会员id'                 AFTER promotion_name;

-- 6.1 联系电话列加长（原为 varchar(11)，员工多输一位号码会导致整单下单失败，放宽到 20 位）
ALTER TABLE orders MODIFY COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号/联系电话（堂食可留11位手机号）';

-- ============================================================
-- 7. 员工赏罚：员工服务评分（星级+小费打赏） + 北极星积分 + 赏罚流水
-- ============================================================
CREATE TABLE IF NOT EXISTS staff_rating (
    id              INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    employee_id     INT          NOT NULL COMMENT '被评价的服务员工id',
    employee_name   VARCHAR(64)  DEFAULT NULL COMMENT '员工姓名（冗余，方便直接展示）',
    order_id        INT          DEFAULT NULL COMMENT '关联堂食订单id（选填）',
    table_no        VARCHAR(32)  DEFAULT NULL COMMENT '桌号/顾客称呼（选填）',
    customer_name   VARCHAR(32)  DEFAULT NULL COMMENT '顾客称呼（选填）',
    service_score   TINYINT      NOT NULL COMMENT '服务态度评分1-5星',
    recommend_score TINYINT      DEFAULT NULL COMMENT '推荐指数（推荐菜品合心意）1-5星',
    content         VARCHAR(500) DEFAULT NULL COMMENT '评价内容/评语',
    tip_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '顾客打赏小费金额',
    star_points     INT          NOT NULL DEFAULT 0 COMMENT '本次评价获得的北极星积分',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效 0隐藏',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_time (employee_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工服务评分表';

CREATE TABLE IF NOT EXISTS staff_reward (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    employee_id  INT          NOT NULL COMMENT '员工id',
    type         TINYINT      NOT NULL COMMENT '类型 1顾客打赏 2店主奖励 3店主惩戒',
    amount       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额：打赏/奖励为正，惩戒为负',
    star_points  INT          NOT NULL DEFAULT 0 COMMENT '北极星积分变动：奖励为正，惩戒为负',
    rating_id    INT          DEFAULT NULL COMMENT '关联的服务评分id（打赏时有值）',
    order_id     INT          DEFAULT NULL COMMENT '关联订单id（选填）',
    reason       VARCHAR(255) DEFAULT NULL COMMENT '奖惩原因/说明',
    operator_id  INT          DEFAULT NULL COMMENT '操作人员工id（店主奖惩时记录）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_time (employee_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工赏罚流水表';

-- ============================================================
-- 8. 惊喜盲盒 + 供应管理
-- ============================================================
CREATE TABLE IF NOT EXISTS blind_box (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '盲盒名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '盲盒售价（固定价，先付款再抽卡）',
    image       VARCHAR(255) DEFAULT NULL COMMENT '盲盒封面图',
    description VARCHAR(255) DEFAULT NULL COMMENT '盲盒说明',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='惊喜盲盒表';

CREATE TABLE IF NOT EXISTS blind_box_option (
    id           INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    box_id       INT          NOT NULL COMMENT '所属盲盒id',
    option_name  VARCHAR(64)  NOT NULL COMMENT '套餐名',
    dish_ids     VARCHAR(255) NOT NULL COMMENT '菜品id逗号串',
    dish_summary VARCHAR(255) DEFAULT NULL COMMENT '菜品摘要冗余',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_box (box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盲盒套餐选项表';

ALTER TABLE orders
    ADD COLUMN blind_box_id INT DEFAULT NULL COMMENT '惊喜盲盒id（盲盒订单才有）' AFTER member_id,
    ADD COLUMN blind_box_option_id INT DEFAULT NULL COMMENT '抽中的盲盒套餐id' AFTER blind_box_id;

CREATE TABLE IF NOT EXISTS supplier (
    id             INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(64)  NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(32)  DEFAULT NULL COMMENT '联系人',
    phone          VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    address        VARCHAR(255) DEFAULT NULL COMMENT '地址',
    main_category  VARCHAR(64)  DEFAULT NULL COMMENT '主营品类',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1合作中 0停用',
    remark         VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原材料供应商表';

CREATE TABLE IF NOT EXISTS material (
    id              INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(64)  NOT NULL COMMENT '原材料名称',
    unit            VARCHAR(16)  NOT NULL DEFAULT '斤' COMMENT '单位',
    reference_price DECIMAL(10,2) DEFAULT NULL COMMENT '参考单价',
    category        VARCHAR(32)  DEFAULT NULL COMMENT '分类',
    supplier_id     INT          DEFAULT NULL COMMENT '常用供应商id',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1在用 0停用',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原材料表';

CREATE TABLE IF NOT EXISTS purchase_record (
    id            INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    supplier_id   INT          NOT NULL COMMENT '供应商id',
    material_id   INT          NOT NULL COMMENT '原材料id',
    quantity      DECIMAL(10,2) NOT NULL COMMENT '采购数量',
    unit_price    DECIMAL(10,2) NOT NULL COMMENT '实际单价',
    total_amount  DECIMAL(10,2) NOT NULL COMMENT '合计金额',
    is_on_time    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否准时到货',
    is_qualified  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否验收合格',
    purchase_time DATETIME     NOT NULL COMMENT '采购/到货时间',
    remark        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_supplier_time (supplier_id, purchase_time),
    KEY idx_material_time (material_id, purchase_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库记录表';
