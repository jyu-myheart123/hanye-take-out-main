<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getDineInCategoryListAPI,
  getDineInDishListAPI,
  dineInSubmitAPI
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

// ===== 计算属性 =====
// 清单总份数
const totalNumber = computed(() =>
  cart.value.reduce((sum, item) => sum + item.number, 0)
)
// 清单总金额（前端实时预览，真正的价格以后端核算为准）
const totalAmount = computed(() =>
  cart.value.reduce((sum, item) => sum + item.price * item.number, 0)
)

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

/** 清空清单和表单（开单成功后用） */
const resetAll = () => {
  cart.value = []
  form.tableNo = ''
  form.phone = ''
  form.remark = ''
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
  submitting.value = true
  try {
    // 只传 id 和份数，价格由后端核算，前端不传金额
    const { data: res } = await dineInSubmitAPI({
      tableNo: form.tableNo,
      phone: form.phone,
      remark: form.remark,
      items: cart.value.map((item) => ({
        dishId: item.dishId,
        number: item.number
      }))
    })
    if (res.code !== 0) return
    ElMessage.success(`开单成功！合计 ¥${Number(res.data.orderAmount).toFixed(2)}，已通知后厨`)
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
          <el-input v-model="form.phone" placeholder="方便后续联系" clearable />
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

      <!-- 合计 + 提交 -->
      <div class="cart-footer">
        <div class="cart-total">
          <span>合计</span>
          <strong>¥{{ totalAmount.toFixed(2) }}</strong>
        </div>
        <el-button
          type="primary"
          size="large"
          class="cart-submit"
          :loading="submitting"
          @click="submit"
        >
          <el-icon style="margin-right: 6px;"><Select /></el-icon>
          提交开单
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

.cart-list {
  max-height: 340px;
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

.cart-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;

  span {
    color: var(--text-sub);
    font-size: 0.95rem;
  }

  strong {
    font-size: 1.7rem;
    color: var(--brand-deep);
    font-variant-numeric: tabular-nums;
  }
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
