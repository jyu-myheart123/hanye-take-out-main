<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPromotionPageAPI,
  addPromotionAPI,
  updatePromotionAPI,
  togglePromotionStatusAPI,
  deletePromotionAPI
} from '@/api/promotion'
import { getDineInCategoryListAPI, getDineInDishListAPI } from '@/api/dinein'

// ===== 常量字典（小白讲解：把1/2/3/4这类魔法数字翻译成中文文案）=====
const TYPE_MAP: Record<number, { label: string; tag: string }> = {
  1: { label: '满减', tag: 'danger' },
  2: { label: '折扣', tag: 'warning' },
  3: { label: '第二份半价', tag: 'success' },
  4: { label: '买一送一', tag: 'primary' }
}
const SCOPE_MAP: Record<number, string> = {
  0: '全场通用',
  1: '指定分类',
  2: '指定菜品'
}

interface Category {
  id: number
  name: string
}
interface Dish {
  id: number
  name: string
  categoryId: number
}

// ===== 列表数据 =====
const tableData = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const queryParams = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined as number | undefined,
  status: undefined as number | undefined
})

// 分类和菜品（配置"指定分类/指定菜品"时用）
const categoryList = ref<Category[]>([])
const dishList = ref<Dish[]>([])

// ===== 弹窗表单 =====
const dialogVisible = ref(false)
const dialogTitle = ref('新增营销活动')
const saving = ref(false)
const form = reactive<any>({
  id: undefined,
  name: '',
  type: 1,
  scopeType: 0,
  scopeIdList: [] as number[],
  tiers: [{ threshold: 30, reduce: 5 }],
  discount: 8.8,
  timeRange: [] as string[],
  status: 1,
  description: ''
})

/** 根据活动类型生成规则摘要，如"满30减5，满60减12" / "全场8.8折" */
const ruleText = (row: any) => {
  if (row.type === 1 && Array.isArray(row.tiers)) {
    return row.tiers
      .map((t: any) => `满${stripZero(t.threshold)}减${stripZero(t.reduce)}`)
      .join('，')
  }
  if (row.type === 2) return `${stripZero(row.discount)}折`
  if (row.type === 3) return '同菜品第2份半价'
  if (row.type === 4) return '同菜品买1送1'
  return '-'
}

/** 数字去掉多余的 .00，让文案好看：30.00 -> 30 */
const stripZero = (n: any) => Number(n || 0).toString()

/** 活动当前所处的时间段状态：未开始/进行中/已结束 */
const timeState = (row: any) => {
  const now = new Date().getTime()
  const begin = new Date(row.beginTime).getTime()
  const end = new Date(row.endTime).getTime()
  if (now < begin) return { text: '未开始', cls: 'state-wait' }
  if (now > end) return { text: '已结束', cls: 'state-end' }
  return { text: '进行中', cls: 'state-live' }
}

/** 加载活动分页列表 */
const loadList = async () => {
  loading.value = true
  try {
    const { data: res } = await getPromotionPageAPI(queryParams)
    if (res.code === 0) {
      tableData.value = res.data.records || []
      total.value = Number(res.data.total)
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  loadList()
}
const handleReset = () => {
  queryParams.name = ''
  queryParams.type = undefined
  queryParams.status = undefined
  queryParams.page = 1
  loadList()
}
const handlePageChange = (p: number) => {
  queryParams.page = p
  loadList()
}

/** 打开新增弹窗 */
const openAdd = () => {
  dialogTitle.value = '新增营销活动'
  Object.assign(form, {
    id: undefined,
    name: '',
    type: 1,
    scopeType: 0,
    scopeIdList: [],
    tiers: [{ threshold: 30, reduce: 5 }],
    discount: 8.8,
    timeRange: [],
    status: 1,
    description: ''
  })
  dialogVisible.value = true
}

/** 打开编辑弹窗（后端已把规则JSON解析成 tiers/discount/scopeIdList） */
const openEdit = async (row: any) => {
  dialogTitle.value = '编辑营销活动'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    scopeType: row.scopeType,
    scopeIdList: row.scopeIdList ? [...row.scopeIdList] : [],
    tiers: Array.isArray(row.tiers) && row.tiers.length
      ? row.tiers.map((t: any) => ({ ...t }))
      : [{ threshold: 30, reduce: 5 }],
    discount: row.discount ?? 8.8,
    timeRange: [row.beginTime, row.endTime],
    status: row.status,
    description: row.description || ''
  })
  dialogVisible.value = true
}

/** 满减档位：加一档 / 删一档 */
const addTier = () => form.tiers.push({ threshold: undefined, reduce: undefined })
const removeTier = (idx: number) => form.tiers.splice(idx, 1)

/** 保存（新增或修改） */
const handleSave = async () => {
  if (!form.name.trim()) return ElMessage.warning('请填写活动名称')
  if (!form.timeRange || form.timeRange.length !== 2) return ElMessage.warning('请选择活动起止时间')
  if (form.scopeType !== 0 && form.scopeIdList.length === 0) {
    return ElMessage.warning(form.scopeType === 1 ? '请选择参与活动的分类' : '请选择参与活动的菜品')
  }
  const payload: any = {
    id: form.id,
    name: form.name.trim(),
    type: form.type,
    scopeType: form.scopeType,
    scopeIdList: form.scopeType === 0 ? [] : form.scopeIdList,
    beginTime: form.timeRange[0],
    endTime: form.timeRange[1],
    status: form.status,
    description: form.description
  }
  if (form.type === 1) payload.tiers = form.tiers
  if (form.type === 2) payload.discount = form.discount

  saving.value = true
  try {
    const api = form.id ? updatePromotionAPI : addPromotionAPI
    const { data: res } = await api(payload)
    if (res.code === 0) {
      ElMessage.success(form.id ? '活动修改成功' : '活动创建成功，开单时自动生效')
      dialogVisible.value = false
      loadList()
    }
  } finally {
    saving.value = false
  }
}

/** 启用/停用 */
const handleToggle = async (row: any) => {
  const { data: res } = await togglePromotionStatusAPI(row.id)
  if (res.code === 0) {
    ElMessage.success(row.status === 1 ? '活动已停用' : '活动已启用')
    loadList()
  }
}

/** 删除（二次确认，防止误删） */
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除活动「${row.name}」吗？删除后开单不再享受该优惠。`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      const { data: res } = await deletePromotionAPI(row.id)
      if (res.code === 0) {
        ElMessage.success('删除成功')
        loadList()
      }
    })
    .catch(() => {})
}

/** 菜品分类选择器：根据scopeType决定可选内容 */
const scopeOptions = computed(() => (form.scopeType === 1 ? categoryList.value : dishList.value))

onMounted(async () => {
  loadList()
  // 并行拉取分类和启售菜品，供"指定范围"选择
  const [catRes, dishRes] = await Promise.all([
    getDineInCategoryListAPI(),
    getDineInDishListAPI()
  ])
  categoryList.value = catRes.data.data || []
  dishList.value = dishRes.data.data || []
})
</script>

<template>
  <div class="promotion-page">
    <el-card class="page-card">
      <!-- 顶部：标题 + 筛选 + 新增 -->
      <div class="page-head">
        <div class="page-head__meta">
          <h2>营销活动</h2>
          <p>配置满减、折扣、第二份半价、买一送一活动，堂食开单时系统自动套用最优惠方案。</p>
        </div>
        <el-button type="primary" @click="openAdd">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>新增活动
        </el-button>
      </div>

      <div class="filter-bar">
        <el-input v-model="queryParams.name" placeholder="活动名称" clearable style="width: 180px"
          @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="queryParams.type" placeholder="活动类型" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="(item, key) in TYPE_MAP" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" plain @click="handleSearch">查询</el-button>
        <el-button plain @click="handleReset">重置</el-button>
      </div>

      <!-- 活动列表 -->
      <el-table v-loading="loading" :data="tableData" stripe class="promotion-table">
        <el-table-column label="活动名称" prop="name" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="(TYPE_MAP[row.type]?.tag as any) || 'info'" effect="light" round>
              {{ TYPE_MAP[row.type]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠规则" min-width="220">
          <template #default="{ row }">
            <span class="rule-text">{{ ruleText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="适用范围" width="110">
          <template #default="{ row }">{{ SCOPE_MAP[row.scopeType] || '全场通用' }}</template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="300">
          <template #default="{ row }">
            <div class="time-cell">
              <span>{{ row.beginTime }} ~ {{ row.endTime }}</span>
              <em :class="timeState(row).cls" v-if="row.status === 1">{{ timeState(row).text }}</em>
              <em class="state-off" v-else>已停用</em>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > queryParams.pageSize"
        class="pager"
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="queryParams.pageSize"
        :current-page="queryParams.page"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form :model="form" label-width="92px">
        <el-form-item label="活动名称" required>
          <el-input v-model="form.name" placeholder="如：午市满减、开业全场8.8折" maxlength="64" />
        </el-form-item>

        <el-form-item label="活动类型" required>
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减</el-radio>
            <el-radio :value="2">折扣</el-radio>
            <el-radio :value="3">第二份半价</el-radio>
            <el-radio :value="4">买一送一</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 满减：多档门槛动态行 -->
        <el-form-item v-if="form.type === 1" label="满减档位" required>
          <div class="tier-box">
            <div v-for="(tier, idx) in form.tiers" :key="idx" class="tier-row">
              <span>满</span>
              <el-input-number v-model="tier.threshold" :min="1" :precision="2" :controls="false" />
              <span>元减</span>
              <el-input-number v-model="tier.reduce" :min="1" :precision="2" :controls="false" />
              <span>元</span>
              <el-button link type="danger" @click="removeTier(idx)" :disabled="form.tiers.length <= 1">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" link @click="addTier">
              <el-icon><Plus /></el-icon>再加一档（自动按最高满足档位减免）
            </el-button>
          </div>
        </el-form-item>

        <!-- 折扣：折数输入 -->
        <el-form-item v-if="form.type === 2" label="折扣力度" required>
          <el-input-number v-model="form.discount" :min="0.1" :max="9.9" :step="0.1" :precision="1" />
          <span class="form-hint">折（如 8.8 表示付原价的88%；可搭配下方"指定分类/菜品"使用）</span>
        </el-form-item>

        <!-- 第二份半价 / 买一送一 说明 -->
        <el-form-item v-if="form.type === 3" label="优惠说明">
          <el-alert type="success" :closable="false" show-icon
            title="同一道菜点2份，第2份半价；点4份则2份半价，开单时按份自动计算。" />
        </el-form-item>
        <el-form-item v-if="form.type === 4" label="优惠说明">
          <el-alert type="success" :closable="false" show-icon
            title="同一道菜点2份只收1份的钱（买1送1）；点3份收2份的钱，开单时自动计算。" />
        </el-form-item>

        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scopeType" @change="form.scopeIdList = []">
            <el-radio :value="0">全场通用</el-radio>
            <el-radio :value="1">指定分类</el-radio>
            <el-radio :value="2">指定菜品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scopeType !== 0" :label="form.scopeType === 1 ? '选择分类' : '选择菜品'">
          <el-select
            v-model="form.scopeIdList"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :placeholder="form.scopeType === 1 ? '选择参与活动的分类' : '选择参与活动的菜品'"
            style="width: 100%"
          >
            <el-option
              v-for="opt in scopeOptions"
              :key="opt.id"
              :label="opt.name"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动时间" required>
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
            active-text="启用" inactive-text="停用" inline-prompt />
        </el-form-item>

        <el-form-item label="活动说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit
            placeholder="给员工看的备注，如：仅闲时可用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.page-card {
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: var(--shadow-md);

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;

  h2 {
    margin: 0 0 6px;
  }

  p {
    margin: 0;
    color: var(--text-sub);
    font-size: 0.9rem;
  }
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin: 18px 0;
  flex-wrap: wrap;
}

.rule-text {
  color: var(--brand-deep);
  font-weight: 700;
}

.time-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.86rem;

  em {
    font-style: normal;
    width: fit-content;
    padding: 1px 8px;
    border-radius: 999px;
    font-size: 0.72rem;
  }

  .state-live {
    color: #157347;
    background: rgba(25, 135, 84, 0.12);
  }

  .state-wait {
    color: #856404;
    background: rgba(255, 193, 7, 0.18);
  }

  .state-end,
  .state-off {
    color: #888;
    background: rgba(0, 0, 0, 0.06);
  }
}

.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

// 满减档位行
.tier-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tier-row {
  display: flex;
  align-items: center;
  gap: 8px;

  .el-input-number {
    width: 120px;
  }
}

.form-hint {
  margin-left: 10px;
  color: var(--text-sub);
  font-size: 0.85rem;
}
</style>
