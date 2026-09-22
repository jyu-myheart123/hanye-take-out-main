<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBlindBoxListAPI,
  saveBlindBoxAPI,
  deleteBlindBoxAPI,
  drawBlindBoxAPI
} from '@/api/blindbox'
import { getDishPageListAPI } from '@/api/dish'

const router = useRouter()
const activeTab = ref('draw')

// ============================================================
// 公共数据：盲盒列表 + 全部菜品（配置套餐用）
// ============================================================
const boxList = ref<any[]>([])
const allDishes = ref<any[]>([])

/** 加载盲盒列表 */
const loadBoxes = async () => {
  const { data: res } = await getBlindBoxListAPI()
  if (res.code === 0) boxList.value = res.data || []
}
/** 加载全部菜品（大页长一次取够，仅用于多选，不做翻页） */
const loadAllDishes = async () => {
  const { data: res } = await getDishPageListAPI({ page: 1, pageSize: 300 })
  if (res.code === 0) allDishes.value = res.data.records || []
}
/** 根据菜品id集合找菜名 */
const dishName = (id: number) => allDishes.value.find((d) => d.id === id)?.name

// 启用中的盲盒（开单时只能选启用的）
const activeBoxes = computed(() => boxList.value.filter((b) => b.status === 1))

// ============================================================
// Tab1：盲盒开单（主流程）
// 阶段：form 填表 → shuffle 洗牌中 → pick 选卡 → result 已出结果
// ============================================================
const stage = ref<'form' | 'shuffle' | 'pick' | 'result'>('form')

const form = reactive({
  boxId: undefined as number | undefined,
  orderType: 2, // 2堂食 1外卖
  tableNo: '', // 堂食：桌号/称呼
  customerName: '', // 堂食顾客称呼 / 外卖收货人
  phone: '',
  address: '',
  remark: ''
})

/** 当前选中的盲盒对象 */
const currentBox = computed(() => boxList.value.find((b) => b.id === form.boxId))

/** 确认收款 → 进入抽卡舞台（先洗牌，再让顾客选卡） */
const goShuffle = () => {
  if (!form.boxId) return ElMessage.warning('请选择一个盲盒')
  if (form.orderType === 2) {
    if (!form.tableNo.trim()) return ElMessage.warning('请填写桌号或顾客称呼，方便后厨叫号')
  } else {
    if (!form.customerName.trim()) return ElMessage.warning('请填写收货人姓名')
    if (!form.phone.trim()) return ElMessage.warning('请填写收货人电话')
    if (!form.address.trim()) return ElMessage.warning('请填写收货地址')
  }
  if (form.phone.trim() && !/^1\d{10}$/.test(form.phone.trim())) {
    return ElMessage.warning('电话格式不正确，请填写11位手机号')
  }

  stage.value = 'shuffle'
  // 洗牌动画播 2.1 秒后进入选卡阶段
  setTimeout(() => {
    stage.value = 'pick'
  }, 2100)
}

// ----- 抽卡 -----
const drawing = ref(false)
const flippedIndices = ref<number[]>([]) // 已翻开的卡位置
const resultData = ref<any>(null) // 抽卡接口返回的数据

/** 顾客/员工点击某张卡牌（只有 pick 阶段能点） */
const pickCard = async (index: number) => {
  if (stage.value !== 'pick' || drawing.value) return
  drawing.value = true
  try {
    const { data: res } = await drawBlindBoxAPI({
      boxId: form.boxId,
      pickedIndex: index,
      orderType: form.orderType,
      tableNo: form.tableNo.trim(),
      customerName: form.customerName.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
      remark: form.remark.trim()
    })
    if (res.code === 0) {
      resultData.value = res.data
      // 稍等 300ms 再翻转，制造"命运揭晓"的停顿感
      setTimeout(() => {
        flippedIndices.value = [index]
        stage.value = 'result'
      }, 300)
    }
  } finally {
    drawing.value = false
  }
}

/** 翻开另外两张"错过的"卡 */
const revealAll = () => {
  flippedIndices.value = [0, 1, 2]
}

/** 某张卡当前是否翻开 */
const isFlipped = (index: number) => flippedIndices.value.includes(index)

/** 再来一盒：全部复位，回到填表阶段（盲盒保留，方便连续卖同款） */
const resetAll = () => {
  stage.value = 'form'
  resultData.value = null
  flippedIndices.value = []
  form.orderType = 2
  form.tableNo = ''
  form.customerName = ''
  form.phone = ''
  form.address = ''
  form.remark = ''
}

/** 跳订单管理看后续流程 */
const goOrder = () => router.push('/order')

// ============================================================
// Tab2：盲盒配置（商家）
// ============================================================
const configVisible = ref(false)
const configSaving = ref(false)
const configForm = reactive<any>({
  id: undefined,
  name: '',
  price: 9.9,
  image: '',
  description: '',
  status: 1,
  options: [
    { optionName: '幸运套餐A', dishIds: [] as number[] },
    { optionName: '惊喜套餐B', dishIds: [] as number[] },
    { optionName: '元气套餐C', dishIds: [] as number[] }
  ]
})

/** 新建盲盒 */
const openCreate = () => {
  Object.assign(configForm, {
    id: undefined,
    name: '',
    price: 9.9,
    image: '',
    description: '',
    status: 1,
    options: [
      { optionName: '幸运套餐A', dishIds: [] },
      { optionName: '惊喜套餐B', dishIds: [] },
      { optionName: '元气套餐C', dishIds: [] }
    ]
  })
  configVisible.value = true
}

/** 编辑现有盲盒（把字符串菜品id拆回数组供多选展示） */
const openEdit = (box: any) => {
  Object.assign(configForm, {
    id: box.id,
    name: box.name,
    price: Number(box.price),
    image: box.image || '',
    description: box.description || '',
    status: box.status,
    options: (box.options || []).map((op: any) => ({
      optionName: op.optionName,
      dishIds: op.dishIds.split(',').map((s: string) => Number(s.trim()))
    }))
  })
  configVisible.value = true
}

/** 保存盲盒配置：把多选数组转成逗号串，并拼出菜品摘要 */
const saveConfig = async () => {
  if (!configForm.name.trim()) return ElMessage.warning('请填写盲盒名称')
  if (!configForm.price || Number(configForm.price) <= 0) return ElMessage.warning('请填写正确的盲盒价格')
  for (const op of configForm.options) {
    if (!op.optionName.trim()) return ElMessage.warning('请填写每个套餐的名称')
    if (!op.dishIds.length) return ElMessage.warning(`套餐【${op.optionName}】还没有选择菜品`)
  }
  configSaving.value = true
  try {
    const payload = {
      id: configForm.id,
      name: configForm.name.trim(),
      price: Number(configForm.price),
      image: configForm.image || null,
      description: configForm.description.trim() || null,
      status: configForm.status,
      options: configForm.options.map((op: any) => ({
        optionName: op.optionName.trim(),
        dishIds: op.dishIds.join(','),
        dishSummary: op.dishIds.map((id: number) => dishName(id)).join(' + ')
      }))
    }
    const { data: res } = await saveBlindBoxAPI(payload)
    if (res.code === 0) {
      ElMessage.success('盲盒配置已保存')
      configVisible.value = false
      loadBoxes()
    }
  } finally {
    configSaving.value = false
  }
}

/** 删除盲盒（二次确认） */
const removeBox = (box: any) => {
  ElMessageBox.confirm(`确定删除盲盒【${box.name}】吗？历史盲盒订单不受影响。`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      const { data: res } = await deleteBlindBoxAPI(box.id)
      if (res.code === 0) {
        ElMessage.success('已删除')
        loadBoxes()
      }
    })
    .catch(() => {})
}

onMounted(() => {
  loadBoxes()
  loadAllDishes()
})
</script>

<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ============== Tab1：盲盒开单 ============== -->
      <el-tab-pane label="盲盒开单" name="draw">
        <!-- 阶段1：填写订单信息 -->
        <div v-if="stage === 'form'" class="form-wrap">
          <el-card shadow="never" class="box-select-card">
            <template #header><b>第一步：请顾客挑选一个盲盒</b></template>
            <el-radio-group v-model="form.boxId" class="box-radio-group">
              <el-radio
                v-for="box in activeBoxes"
                :key="box.id"
                :value="box.id"
                class="box-radio"
                border
              >
                <div class="box-radio-inner">
                  <span class="box-emoji">🎁</span>
                  <span class="box-name">{{ box.name }}</span>
                  <span class="box-price">¥{{ Number(box.price).toFixed(2) }}</span>
                  <span class="box-desc">{{ box.description }}</span>
                </div>
              </el-radio>
            </el-radio-group>
            <el-empty v-if="!activeBoxes.length" description="还没有启用的盲盒，请先到“盲盒配置”创建" />
          </el-card>

          <el-card v-if="form.boxId" shadow="never" class="order-info-card">
            <template #header><b>第二步：填写订单信息（收款后才能抽卡哦）</b></template>
            <el-radio-group v-model="form.orderType" class="type-group">
              <el-radio-button :value="2">🍚 堂食</el-radio-button>
              <el-radio-button :value="1">🛵 外卖</el-radio-button>
            </el-radio-group>

            <!-- 堂食信息 -->
            <el-form v-if="form.orderType === 2" label-position="top" style="max-width: 520px">
              <el-form-item label="桌号 / 顾客称呼（必填，后厨叫号用）">
                <el-input v-model="form.tableNo" placeholder="如：A3桌 / 王先生" maxlength="32" />
              </el-form-item>
              <el-form-item label="联系电话（选填）">
                <el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" />
              </el-form-item>
            </el-form>

            <!-- 外卖信息 -->
            <el-form v-else label-position="top" style="max-width: 520px">
              <el-form-item label="收货人（必填）">
                <el-input v-model="form.customerName" maxlength="32" />
              </el-form-item>
              <el-form-item label="电话（必填）">
                <el-input v-model="form.phone" maxlength="11" />
              </el-form-item>
              <el-form-item label="收货地址（必填）">
                <el-input v-model="form.address" type="textarea" :rows="2" />
              </el-form-item>
            </el-form>

            <el-form label-position="top" style="max-width: 520px">
              <el-form-item label="备注（选填）">
                <el-input v-model="form.remark" placeholder="如：不要辣" maxlength="255" />
              </el-form-item>
            </el-form>
          </el-card>

          <div v-if="form.boxId" class="pay-bar">
            <span class="pay-label">应收盲盒款</span>
            <span class="pay-amount">¥{{ Number(currentBox?.price).toFixed(2) }}</span>
            <el-button type="primary" size="large" round @click="goShuffle">
              确认收款，开始抽卡 🃏
            </el-button>
          </div>
        </div>

        <!-- 阶段2/3：卡牌舞台（洗牌 → 选卡 → 翻转） -->
        <div v-else class="card-stage">
          <div class="stage-title">
            <template v-if="stage === 'shuffle'">命运洗牌中…… ✨</template>
            <template v-else-if="stage === 'pick'">
              第三步：请顾客<b>选择一张命运卡牌</b>（已收款 ¥{{ Number(currentBox?.price).toFixed(2) }}）
            </template>
            <template v-else>🎉 恭喜！命运套餐已揭晓，订单已进入后厨流程</template>
          </div>

          <div class="cards-row">
            <div
              v-for="i in 3"
              :key="i"
              :class="[
                'flip-card',
                stage === 'shuffle' ? 'shuffling' : '',
                stage === 'pick' ? 'pickable' : '',
                isFlipped(i - 1) ? 'flipped' : '',
                resultData && i - 1 === resultData.wonIndex ? 'is-won' : '',
                resultData && isFlipped(i - 1) && i - 1 !== resultData.wonIndex ? 'is-lost' : ''
              ]"
              :style="{ animationDelay: stage === 'shuffle' ? `${(i - 1) * 0.18}s` : '' }"
              @click="pickCard(i - 1)"
            >
              <div class="card-inner">
                <!-- 卡牌背面 -->
                <div class="card-face card-back">
                  <span class="back-emoji">🎁</span>
                  <span class="back-q">?</span>
                  <span class="back-name">{{ currentBox?.name }}</span>
                  <span class="back-hint">点我揭晓</span>
                </div>
                <!-- 卡牌正面 -->
                <div class="card-face card-front">
                  <template v-if="resultData">
                    <div class="front-title">
                      {{ resultData.cards[i - 1].optionName }}
                    </div>
                    <div v-if="i - 1 === resultData.wonIndex" class="won-tag">⭐ 命运套餐 ⭐</div>
                    <div v-else class="lost-tag">错过了它</div>
                    <div class="front-dishes">
                      <div v-for="dish in resultData.cards[i - 1].dishes" :key="dish.dishId" class="front-dish">
                        <img v-if="dish.pic" :src="dish.pic" class="dish-pic" alt="" />
                        <span v-else class="dish-pic-placeholder">🍲</span>
                        <span class="dish-name-text">{{ dish.name }}</span>
                        <span class="dish-price">¥{{ Number(dish.price).toFixed(2) }}</span>
                      </div>
                    </div>
                    <div class="front-footer">
                      <span class="front-original">原价 ¥{{ Number(resultData.cards[i - 1].dishes.reduce((s: number, d: any) => s + Number(d.price), 0)).toFixed(2) }}</span>
                      <span v-if="i - 1 === resultData.wonIndex" class="front-pay">
                        实付 ¥{{ Number(resultData.boxPrice).toFixed(2) }}
                      </span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <!-- 结果操作区 -->
          <div v-if="stage === 'result'" class="result-actions">
            <el-button v-if="flippedIndices.length < 3" size="large" round @click="revealAll">
              翻开另外两张
            </el-button>
            <el-button size="large" type="success" round @click="goOrder">
              去订单管理查看 👀
            </el-button>
            <el-button size="large" type="primary" round @click="resetAll">
              再来一盒 🎁
            </el-button>
            <p class="order-number">订单号：{{ resultData?.orderNumber }}</p>
          </div>
        </div>
      </el-tab-pane>

      <!-- ============== Tab2：盲盒配置 ============== -->
      <el-tab-pane label="盲盒配置" name="config">
        <div class="config-toolbar">
          <el-button type="primary" @click="openCreate">＋ 新建盲盒</el-button>
        </div>
        <el-table :data="boxList" stripe>
          <el-table-column label="盲盒名称" prop="name" width="200" />
          <el-table-column label="价格" width="100">
            <template #default="{ row }">
              <b class="price-text">¥{{ Number(row.price).toFixed(2) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="3个套餐选项" min-width="320">
            <template #default="{ row }">
              <el-tag
                v-for="(op, idx) in row.options"
                :key="idx"
                size="small"
                class="option-tag"
                type="info"
              >
                {{ op.optionName }}：{{ op.dishSummary }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" round>
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeBox(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 盲盒配置弹窗 ===== -->
    <el-dialog
      v-model="configVisible"
      :title="configForm.id ? '编辑盲盒' : '新建盲盒'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <div class="cfg-row">
          <el-form-item label="盲盒名称" style="flex:2">
            <el-input v-model="configForm.name" placeholder="如：9.9元惊喜盲盒" maxlength="64" />
          </el-form-item>
          <el-form-item label="盲盒价格(元)" style="flex:1">
            <el-input-number
              v-model="configForm.price"
              :min="0.01"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <el-form-item label="盲盒说明">
          <el-input v-model="configForm.description" placeholder="如：一荤一素一饭，今天吃什么交给运气" maxlength="255" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="configForm.status" :active-value="1" :inactive-value="0"
            active-text="启用" inactive-text="停用" />
        </el-form-item>

        <el-divider content-position="left">3个套餐选项（抽卡池）</el-divider>

        <div v-for="(op, idx) in configForm.options" :key="idx" class="cfg-option-block">
            <el-form-item :label="`套餐${idx + 1}名称`">
              <el-input v-model="op.optionName" placeholder="如：幸运套餐A" maxlength="64" />
            </el-form-item>
            <el-form-item label="选择套餐内菜品（可多选）">
              <el-select
                v-model="op.dishIds"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                placeholder="点击选择菜品（如一荤一素一饭）"
                style="width: 100%"
              >
                <el-option
                  v-for="dish in allDishes"
                  :key="dish.id"
                  :label="`${dish.name}（¥${Number(dish.price).toFixed(2)}）`"
                  :value="dish.id"
                />
              </el-select>
            </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSaving" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.form-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.box-select-card,
.order-info-card {
  max-width: 900px;
}
.box-radio-group {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}
.box-radio {
  margin: 0 !important;
  height: auto;
  padding: 14px 18px;
  border-radius: 12px;
}
.box-radio-inner {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 180px;
}
.box-emoji {
  font-size: 28px;
}
.box-name {
  font-size: 16px;
  font-weight: 700;
}
.box-price {
  color: #e6532b;
  font-size: 18px;
  font-weight: 700;
}
.box-desc {
  color: #909399;
  font-size: 12px;
}
.type-group {
  margin-bottom: 16px;
}
.pay-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 900px;
  padding: 16px 20px;
  background: linear-gradient(90deg, #fff7ed, #fff);
  border-radius: 12px;
}
.pay-label {
  color: #909399;
}
.pay-amount {
  font-size: 30px;
  font-weight: 800;
  color: #e6532b;
  margin-right: auto;
}

/* ===== 卡牌舞台 ===== */
.card-stage {
  padding: 10px 0;
}
.stage-title {
  text-align: center;
  font-size: 18px;
  margin-bottom: 30px;
  color: #303133;
}
.cards-row {
  display: flex;
  justify-content: center;
  gap: 40px;
  perspective: 1400px;
  flex-wrap: wrap;
}
.flip-card {
  width: 230px;
  height: 340px;
}
.card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.9s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}
.flip-card.flipped .card-inner {
  transform: rotateY(180deg);
}
.card-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: 18px;
  overflow: hidden;
}
.card-back {
  background: linear-gradient(160deg, #7c3aed 0%, #a855f7 50%, #6d28d9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  box-shadow: 0 10px 30px rgba(124, 58, 237, 0.35);
  border: 3px solid rgba(255, 255, 255, 0.25);
}
.back-emoji {
  font-size: 52px;
}
.back-q {
  font-size: 40px;
  font-weight: 800;
}
.back-name {
  font-size: 14px;
  opacity: 0.9;
}
.back-hint {
  margin-top: 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 14px;
  border-radius: 20px;
}
.pickable {
  cursor: pointer;
  transition: transform 0.2s;
}
.pickable:hover {
  transform: translateY(-12px);
}
.pickable:hover .card-back {
  box-shadow: 0 18px 40px rgba(124, 58, 237, 0.5);
}
.card-front {
  transform: rotateY(180deg);
  background: #fff;
  border: 2px solid #ede9fe;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
}
.flip-card.is-won .card-front {
  border: 3px solid #f59e0b;
  box-shadow: 0 0 32px rgba(245, 158, 11, 0.55);
}
.flip-card.is-lost .card-front {
  filter: grayscale(0.8);
  opacity: 0.75;
}
.front-title {
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  padding: 10px 0 4px;
  color: #4c1d95;
}
.won-tag {
  text-align: center;
  color: #b45309;
  font-size: 13px;
  font-weight: 700;
}
.lost-tag {
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}
.front-dishes {
  flex: 1;
  padding: 10px 14px;
  overflow-y: auto;
}
.front-dish {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #f0edff;
}
.dish-pic,
.dish-pic-placeholder {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f3ff;
  font-size: 20px;
}
.dish-name-text {
  flex: 1;
  font-size: 13px;
}
.dish-price {
  font-size: 12px;
  color: #e6532b;
}
.front-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px;
  background: #faf5ff;
}
.front-original {
  font-size: 12px;
  color: #9ca3af;
  text-decoration: line-through;
}
.front-pay {
  font-size: 18px;
  font-weight: 800;
  color: #e6532b;
}

/* 洗牌动画 */
.shuffling {
  animation: shuffleMove 0.7s ease-in-out 3;
}
@keyframes shuffleMove {
  0% { transform: translateX(0) rotate(0); }
  25% { transform: translateX(-46px) translateY(-14px) rotate(-4deg); }
  50% { transform: translateX(42px) translateY(10px) rotate(4deg); }
  75% { transform: translateX(-20px) translateY(-6px) rotate(-2deg); }
  100% { transform: translateX(0) rotate(0); }
}

.result-actions {
  margin-top: 36px;
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}
.order-number {
  width: 100%;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.config-toolbar {
  margin-bottom: 14px;
}
.price-text {
  color: #e6532b;
  font-size: 16px;
}
.option-tag {
  margin: 2px 4px 2px 0;
}
.cfg-row {
  display: flex;
  gap: 16px;
}
.cfg-option-block {
  border: 1px solid #ede9fe;
  border-radius: 12px;
  padding: 12px 16px 0;
  margin-bottom: 14px;
  background: #fcfaff;
}
</style>
