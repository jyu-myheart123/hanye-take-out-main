<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getDineInCategoryListAPI,
  getDineInDishListAPI,
  dineInSubmitAPI,
  dineInPreviewAPI
} from '@/api/dinein'

// ===== 类型约定（小白讲解：interface 就是给数据约定一个形状，方便写代码时有提示）=====
interface Category {
  id: number
  name: string
  type: number
}
interface Dish {
  id: number
  name: string
  pic: string
  price: number
  categoryId: number
}
// 开单清单里的一项 = 一份菜品 + 点了几份
interface CartItem {
  dishId: number
  name: string
  pic: string
  price: number
  number: number
}

const router = useRouter()

// ===== 数据 =====
const categoryList = ref<Category[]>([])
const dishList = ref<Dish[]>([])
const activeCategoryId = ref<number | undefined>(undefined) // undefined 表示"全部"
const cart = ref<CartItem[]>([])
const submitting = ref(false)

// 开单表单：桌号/称呼、电话（选填）、备注
const form = reactive({
  tableNo: '',
  phone: '',
  remark: ''
})

// ===== 会员与优惠相关状态 =====
const memberPhone = ref('')              // 会员手机号（顾客报号，员工输入）
const payMethod = ref(1)                 // 支付方式：1=现金/扫码，2=会员余额
const usePoints = ref(false)             // 是否使用积分抵扣
const priceInfo = ref<any>(null)         // 后端试算返回的完整价格明细
let previewTimer: any = null             // 防抖计时器（连续加菜只请求一次）

// ===== 计算属性 =====
// 清单总份数
const totalNumber = computed(() =>
  cart.value.reduce((sum, item) => sum + item.number, 0)
)
// 清单原价总金额（仅在还没拿到后端试算结果时临时显示，真正价格以后端为准）
const totalAmount = computed(() =>
  cart.value.reduce((sum, item) => sum + item.price * item.number, 0)
)
// 手机号格式是否正确（1开头的11位数字）
const isValidPhone = computed(() => /^1\d{10}$/.test(memberPhone.value.trim()))
// 是否已识别到会员（后端试算结果里带会员id才算）
const isMember = computed(() => !!priceInfo.value?.memberId)
// 余额是否足够支付当前实付金额
const balanceEnough = computed(() => {
  if (!priceInfo.value) return false
  return Number(priceInfo.value.balance) >= Number(priceInfo.value.payAmount)
})

// ===== 方法 =====
/** 页面加载：拉取分类 + 全部启售菜品 */
const init = async () => {
  const [catRes, dishRes] = await Promise.all([
    getDineInCategoryListAPI(),
    getDineInDishListAPI()
  ])
  categoryList.value = catRes.data.data || []
  dishList.value = dishRes.data.data || []
}

/** 点击分类标签：切换分类并重新拉该分类下的菜品 */
const switchCategory = async (categoryId: number | undefined) => {
  activeCategoryId.value = categoryId
  const { data: res } = await getDineInDishListAPI(categoryId)
  dishList.value = res.data || []
}

/** 把菜品加入清单：已存在就份数 +1，不存在就新加一项 */
const addToCart = (dish: Dish) => {
  const exist = cart.value.find((item) => item.dishId === dish.id)
  if (exist) {
    exist.number += 1
  } else {
    cart.value.push({
      dishId: dish.id,
      name: dish.name,
      pic: dish.pic,
      price: Number(dish.price),
      number: 1
    })
  }
}

/** 清单里加一份 */
const plus = (item: CartItem) => {
  item.number += 1
}

/** 清单里减一份，减到 0 就移除 */
const minus = (item: CartItem) => {
  if (item.number > 1) {
    item.number -= 1
  } else {
    cart.value = cart.value.filter((x) => x.dishId !== item.dishId)
  }
}

/** 从清单移除某道菜 */
const removeItem = (item: CartItem) => {
  cart.value = cart.value.filter((x) => x.dishId !== item.dishId)
}

/** 组装给后端的开单参数（试算和提交共用，保证两次算账口径完全一致） */
const buildPayload = () => ({
  items: cart.value.map((item) => ({
    dishId: item.dishId,
    number: item.number
  })),
  // 手机号格式正确才传，否则当散客处理
  memberPhone: isValidPhone.value ? memberPhone.value.trim() : '',
  usePoints: usePoints.value
})

/**
 * 价格试算（防抖350毫秒）
 * 小白讲解：加菜、改手机号、勾选积分后，自动让后端重新算一遍账：
 * 原价→活动优惠→会员等级折扣→积分抵扣→实付，前端只负责展示
 */
const requestPreview = () => {
  clearTimeout(previewTimer)
  if (cart.value.length === 0) {
    priceInfo.value = null
    return
  }
  previewTimer = setTimeout(async () => {
    try {
      const { data: res } = await dineInPreviewAPI(buildPayload())
      if (res.code === 0) priceInfo.value = res.data
    } catch (e) {
      // 试算失败不打断点菜，保留上一次结果
    }
  }, 350)
}

// 清单/手机号/积分勾选有变化就重新试算（deep 深度监听清单里份数的变化）
watch([cart, memberPhone, usePoints], requestPreview, { deep: true })

// 手机号不合法时，清掉会员专属选项，防止误提交
watch(memberPhone, (val) => {
  if (!/^1\d{10}$/.test(val.trim())) {
    usePoints.value = false
    payMethod.value = 1
  }
})

// 试算结果显示不是会员时（新手机号），自动切回现金支付并取消积分勾选
watch(priceInfo, (info) => {
  if (info && !info.memberId) {
    payMethod.value = 1
    usePoints.value = false
  }
})

/** 清空清单和表单（开单成功后用） */
const resetAll = () => {
  cart.value = []
  form.tableNo = ''
  form.phone = ''
  form.remark = ''
  memberPhone.value = ''
  payMethod.value = 1
  usePoints.value = false
  priceInfo.value = null
}

/** 提交开单 */
const submit = async () => {
  // 前端校验：清单不能为空、桌号/称呼要填（后厨要叫号）
  if (cart.value.length === 0) {
    ElMessage.warning('请先为客户添加菜品')
    return
  }
  if (!form.tableNo.trim()) {
    ElMessage.warning('请填写桌号或客户称呼，方便后厨叫号')
    return
  }
  // 联系电话选填，但只要填了就必须是11位手机号（避免多输一位导致下单失败）
  const contactPhone = form.phone.trim()
  if (contactPhone && !/^1\d{10}$/.test(contactPhone)) {
    ElMessage.warning('联系电话格式不正确，请填写11位手机号，或清空留空')
    return
  }
  // 选了余额支付却没识别到会员 / 余额不足时的友好提示（后端也会再拦一道）
  if (payMethod.value === 2) {
    if (!isMember.value) {
      ElMessage.warning('请先输入正确的会员手机号，才能使用余额支付')
      return
    }
    if (!balanceEnough.value) {
      ElMessage.warning('会员余额不足，请改用现金/扫码，或先到「堂食会员」充值')
      return
    }
  }
  submitting.value = true
  try {
    // 只传 id、份数、会员手机号、支付方式和积分勾选，金额一律由后端核算
    const { data: res } = await dineInSubmitAPI({
      tableNo: form.tableNo,
      phone: form.phone,
      remark: form.remark,
      items: cart.value.map((item) => ({
        dishId: item.dishId,
        number: item.number
      })),
      memberPhone: isValidPhone.value ? memberPhone.value.trim() : '',
      payMethod: payMethod.value,
      usePoints: usePoints.value
    })
    if (res.code !== 0) return
    const p = res.data
    // 汇总一下这单一共省了多少钱（活动+会员折扣+积分）
    const saved =
      Number(p.discountAmount || 0) +
      Number(p.memberDiscount || 0) +
      Number(p.pointsDeduction || 0)
    let msg = `开单成功！实付 ¥${Number(p.payAmount).toFixed(2)}`
    if (saved > 0) msg += `（本单共省 ¥${saved.toFixed(2)}）`
    msg += '，已通知后厨'
    // 会员单再提示返积分和余额剩余，让员工能直接告诉顾客
    if (p.memberId) {
      msg += `；本单返 ${p.pointsEarned || 0} 积分，当前共 ${p.pointsAfter} 分`
      if (payMethod.value === 2) msg += `，余额剩余 ¥${Number(p.balanceAfter).toFixed(2)}`
    }
    ElMessage.success(msg)
    resetAll()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  init()
})
</script>

<template>
  <div class="dinein-page">
    <!-- 左侧：点菜区 -->
    <el-card class="dinein-card dish-card">
      <div class="page-head">
        <div class="page-head__meta">
          <h2>堂食开单</h2>
          <p>为到店客户现场点菜开单，价格自动核算，提交后直接进入后厨接单队列。</p>
        </div>
      </div>

      <!-- 分类标签 -->
      <div class="category-tabs">
        <span
          class="category-tab"
          :class="{ active: activeCategoryId === undefined }"
          @click="switchCategory(undefined)"
        >
          全部
        </span>
        <span
          v-for="cat in categoryList"
          :key="cat.id"
          class="category-tab"
          :class="{ active: activeCategoryId === cat.id }"
          @click="switchCategory(cat.id)"
        >
          {{ cat.name }}
        </span>
      </div>

      <!-- 菜品网格 -->
      <div class="dish-grid">
        <div v-for="dish in dishList" :key="dish.id" class="dish-item">
          <div class="dish-pic">
            <img v-if="dish.pic" :src="dish.pic" :alt="dish.name" />
            <div v-else class="dish-pic--empty">
              <el-icon><Dish /></el-icon>
            </div>
          </div>
          <div class="dish-info">
            <p class="dish-name">{{ dish.name }}</p>
            <div class="dish-bottom">
              <span class="dish-price">¥{{ Number(dish.price).toFixed(2) }}</span>
              <el-button
                type="primary"
                circle
                size="small"
                class="dish-add"
                @click="addToCart(dish)"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <el-empty
          v-if="dishList.length === 0"
          description="该分类下暂无启售菜品"
          class="dish-empty"
        />
      </div>
    </el-card>

    <!-- 右侧：开单清单 -->
    <el-card class="dinein-card cart-card">
      <div class="cart-head">
        <h3>
          <el-icon><Tickets /></el-icon>
          开单清单
        </h3>
        <el-tag v-if="totalNumber > 0" type="warning" round>{{ totalNumber }} 份</el-tag>
      </div>

      <!-- 桌号/电话/备注 -->
      <el-form label-position="top" class="cart-form">
        <el-form-item label="桌号 / 客户称呼（必填，后厨叫号用）">
          <el-input v-model="form.tableNo" placeholder="如：A3桌 / 王先生" clearable />
        </el-form-item>
        <el-form-item label="联系电话（选填）">
          <el-input v-model="form.phone" maxlength="11" placeholder="方便后续联系，填则须为11位手机号" clearable />
        </el-form-item>
        <el-form-item label="备注（选填）">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="如：不要辣、孩子吃少放盐"
          />
        </el-form-item>
      </el-form>

      <!-- 会员识别区：顾客报手机号，输完自动识别并带出等级/余额/积分 -->
      <div class="member-box">
        <el-input v-model="memberPhone" maxlength="11" placeholder="会员手机号（选填，老顾客报号）" clearable>
          <template #prefix>
            <el-icon><GoldMedal /></el-icon>
          </template>
        </el-input>
        <!-- 手机号还没输对 -->
        <div v-if="memberPhone && !isValidPhone" class="member-hint member-hint--warn">
          请输入正确的11位手机号
        </div>
        <!-- 手机号格式对，但系统里没这个会员 -->
        <div v-else-if="isValidPhone && !isMember" class="member-hint">
          未查到该会员，结账后将自动建档成为银卡会员
        </div>
        <!-- 识别成功：展示会员卡片 -->
        <div v-else-if="isMember" class="member-card">
          <div class="member-card__top">
            <strong>{{ priceInfo.memberName || '会员顾客' }}</strong>
            <el-tag type="warning" effect="dark" round size="small">
              {{ priceInfo.memberLevelName || '银卡会员' }}
            </el-tag>
          </div>
          <div class="member-card__stats">
            <span><el-icon><Wallet /></el-icon>余额 ¥{{ Number(priceInfo.balance).toFixed(2) }}</span>
            <span><el-icon><Coin /></el-icon>{{ priceInfo.points }} 积分</span>
          </div>
          <div v-if="Number(priceInfo.levelDiscount) < 1" class="member-card__discount">
            本单在活动价基础上再享 {{ Number(priceInfo.levelDiscount) * 10 }} 折
          </div>
        </div>
      </div>

      <!-- 清单列表 -->
      <div class="cart-list">
        <div v-for="item in cart" :key="item.dishId" class="cart-item">
          <div class="cart-item__pic">
            <img v-if="item.pic" :src="item.pic" :alt="item.name" />
            <div v-else class="cart-item__pic--empty">
              <el-icon><Dish /></el-icon>
            </div>
          </div>
          <div class="cart-item__info">
            <p class="cart-item__name">{{ item.name }}</p>
            <p class="cart-item__price">¥{{ item.price.toFixed(2) }}</p>
          </div>
          <div class="cart-item__actions">
            <el-button circle size="small" @click="minus(item)">
              <el-icon><Minus /></el-icon>
            </el-button>
            <span class="cart-item__num">{{ item.number }}</span>
            <el-button circle size="small" type="primary" @click="plus(item)">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
          <p class="cart-item__subtotal">¥{{ (item.price * item.number).toFixed(2) }}</p>
          <el-button
            link
            type="danger"
            class="cart-item__remove"
            @click="removeItem(item)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-empty v-if="cart.length === 0" description="还没点菜，点击左侧菜品 + 号开单" :image-size="80" />
      </div>

      <!-- 支付方式 + 积分抵扣（会员才显示） -->
      <div v-if="isMember" class="pay-box">
        <el-radio-group v-model="payMethod" size="small">
          <el-radio-button :value="1">现金/扫码</el-radio-button>
          <el-radio-button :value="2">余额支付</el-radio-button>
        </el-radio-group>
        <el-checkbox
          v-model="usePoints"
          :disabled="!(Number(priceInfo?.maxPointsDeduction) > 0)"
          class="points-check"
        >
          使用积分抵扣（当前 {{ priceInfo?.points }} 分，本单最多抵 ¥{{ Number(priceInfo?.maxPointsDeduction || 0).toFixed(2) }}，100分抵5元）
        </el-checkbox>
        <div v-if="payMethod === 2 && !balanceEnough" class="pay-warn">
          余额不足！差额请收现金，或先到「堂食会员」充值
        </div>
      </div>

      <!-- 价格明细 + 提交 -->
      <div class="cart-footer">
        <div class="price-box" v-if="priceInfo">
          <!-- 原价 -->
          <div class="price-row">
            <span>原价合计</span>
            <b>¥{{ Number(priceInfo.originalAmount).toFixed(2) }}</b>
          </div>
          <!-- 营销活动优惠 -->
          <template v-if="priceInfo.promotionId">
            <div class="price-row price-row--off">
              <span>活动优惠（{{ priceInfo.promotionName }}）</span>
              <b>-¥{{ Number(priceInfo.discountAmount).toFixed(2) }}</b>
            </div>
            <!-- 每道菜的优惠提示，如：宫保鸡丁 第二份半价 优惠¥9.00 -->
            <div v-for="(tip, idx) in priceInfo.tips" :key="idx" class="price-tip">
              <el-icon><Present /></el-icon>{{ tip }}
            </div>
          </template>
          <!-- 会员等级折扣 -->
          <div v-if="Number(priceInfo.memberDiscount) > 0" class="price-row price-row--off">
            <span>会员折扣（{{ priceInfo.memberLevelName }}）</span>
            <b>-¥{{ Number(priceInfo.memberDiscount).toFixed(2) }}</b>
          </div>
          <!-- 积分抵扣 -->
          <div v-if="Number(priceInfo.pointsDeduction) > 0" class="price-row price-row--off">
            <span>积分抵扣</span>
            <b>-¥{{ Number(priceInfo.pointsDeduction).toFixed(2) }}</b>
          </div>
          <!-- 实付：大字醒目 -->
          <div class="price-row price-row--pay">
            <span>实付金额</span>
            <strong>¥{{ Number(priceInfo.payAmount).toFixed(2) }}</strong>
          </div>
        </div>
        <!-- 还没拿到后端试算结果时，先用前端合计兜底显示 -->
        <div class="price-box" v-else>
          <div class="price-row price-row--pay">
            <span>合计</span>
            <strong>¥{{ totalAmount.toFixed(2) }}</strong>
          </div>
        </div>

        <el-button
          type="primary"
          size="large"
          class="cart-submit"
          :loading="submitting"
          @click="submit"
        >
          <el-icon style="margin-right: 6px;"><Select /></el-icon>
          确认提交开单
        </el-button>
        <el-button size="large" plain @click="router.push('/order')">
          去订单管理查看
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style lang="less" scoped>
// 页面整体：左右两栏布局（左点菜区弹性更宽，右清单固定宽度）
.dinein-page {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.dinein-card {
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: var(--shadow-md);

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.dish-card {
  flex: 1;
  min-width: 0;
}

.cart-card {
  width: 400px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
}

// 分类标签：横向胶囊，选中态用品牌橙
.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0;
}

.category-tab {
  padding: 7px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--line-soft);
  color: var(--text-sub);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: var(--brand-deep);
    border-color: rgba(239, 143, 53, 0.4);
  }

  &.active {
    background: linear-gradient(135deg, rgba(239, 143, 53, 0.96), rgba(221, 107, 32, 0.92));
    border-color: transparent;
    color: #fff;
    box-shadow: 0 8px 18px rgba(239, 143, 53, 0.28);
  }
}

// 菜品网格：自适应列数，窗口窄了自动折行
.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.dish-item {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg);
  padding: 12px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-md);
    border-color: rgba(239, 143, 53, 0.35);
  }
}

.dish-pic {
  width: 100%;
  height: 110px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 10px;
  background: var(--brand-soft);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &--empty {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2rem;
    color: var(--brand-deep);
  }
}

.dish-info {
  .dish-name {
    margin: 0 0 8px;
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--text-main);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .dish-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .dish-price {
    color: var(--brand-deep);
    font-weight: 800;
    font-size: 1.05rem;
    font-variant-numeric: tabular-nums;
  }

  .dish-add {
    background: linear-gradient(135deg, var(--brand), #dd6b20);
    border: none;
  }
}

.dish-empty {
  grid-column: 1 / -1;
}

// 右侧清单
.cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 1.1rem;
    color: var(--text-main);
  }
}

.cart-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}

// 会员识别区
.member-box {
  margin-bottom: 12px;
}

.member-hint {
  margin-top: 6px;
  font-size: 0.78rem;
  color: var(--text-sub);

  &--warn {
    color: #e6a23c;
  }
}

.member-card {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(239, 143, 53, 0.12), rgba(221, 107, 32, 0.06));
  border: 1px solid rgba(239, 143, 53, 0.25);

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;

    strong {
      font-size: 0.95rem;
      color: var(--text-main);
    }
  }

  &__stats {
    display: flex;
    gap: 16px;
    margin-top: 8px;
    font-size: 0.85rem;
    color: var(--text-main);
    font-variant-numeric: tabular-nums;

    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  &__discount {
    margin-top: 6px;
    font-size: 0.78rem;
    color: var(--brand-deep);
    font-weight: 600;
  }
}

// 支付方式区
.pay-box {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px dashed var(--line-soft);

  .points-check {
    display: flex;
    margin-top: 8px;
    margin-right: 0;
    height: auto;
    align-items: flex-start;
    white-space: normal;

    :deep(.el-checkbox__label) {
      font-size: 0.8rem;
      line-height: 1.4;
    }
  }
}

.pay-warn {
  margin-top: 6px;
  font-size: 0.78rem;
  color: #dc3545;
}

.cart-list {
  max-height: 260px;
  overflow-y: auto;
  margin: 8px 0 16px;
  padding-right: 4px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-bottom: 1px dashed var(--line-soft);

  &__pic {
    width: 46px;
    height: 46px;
    border-radius: 10px;
    overflow: hidden;
    background: var(--brand-soft);
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &--empty {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--brand-deep);
    }
  }

  &__info {
    width: 90px;
    min-width: 0;
  }

  &__name {
    margin: 0;
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--text-main);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__price {
    margin: 2px 0 0;
    font-size: 0.8rem;
    color: var(--text-sub);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__num {
    min-width: 20px;
    text-align: center;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }

  &__subtotal {
    margin: 0;
    width: 74px;
    text-align: right;
    font-weight: 800;
    color: var(--brand-deep);
    font-variant-numeric: tabular-nums;
  }

  &__remove {
    padding: 4px;
  }
}

.cart-footer {
  border-top: 1px solid var(--line-soft);
  padding-top: 16px;
}

// 价格明细区：原价/活动优惠/会员折扣/积分抵扣/实付
.price-box {
  margin-bottom: 14px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 247, 240, 0.6);
}

.price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 0.88rem;
  color: var(--text-sub);
  line-height: 1.9;

  b {
    font-weight: 600;
    color: var(--text-main);
    font-variant-numeric: tabular-nums;
  }

  // 优惠行：金额用绿色，表示帮顾客省了钱
  &--off b {
    color: #157347;
  }

  // 实付行：大字品牌橙
  &--pay {
    margin-top: 4px;
    padding-top: 8px;
    border-top: 1px dashed var(--line-soft);

    span {
      color: var(--text-main);
      font-weight: 600;
      font-size: 0.95rem;
    }

    strong {
      font-size: 1.7rem;
      color: var(--brand-deep);
      font-variant-numeric: tabular-nums;
    }
  }
}

// 每道菜的优惠提示（第二份半价/买一送一，让员工能直接念给顾客听）
.price-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.76rem;
  color: #157347;
  line-height: 1.6;
  padding-left: 4px;
}

.cart-submit {
  width: 100%;
  margin-bottom: 10px;
  background: linear-gradient(135deg, var(--brand), #dd6b20);
  border: none;
  font-size: 1rem;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(239, 143, 53, 0.28);
}

.cart-footer .el-button--large:last-child {
  width: 100%;
}

// 窄屏：上下堆叠
@media (max-width: 1100px) {
  .dinein-page {
    flex-direction: column;
  }

  .cart-card {
    width: 100%;
    position: static;
  }
}
</style>
