<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMemberPageAPI,
  addMemberAPI,
  rechargeMemberAPI,
  adjustMemberPointsAPI,
  getMemberFlowAPI,
  getMemberRulesAPI,
  updateMemberRulesAPI
} from '@/api/member'

// 等级标签样式（银=info 灰，金=warning，钻石=primary）
const LEVEL_TAG: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary' }

// 流水类型字典
const FLOW_TYPE_MAP: Record<number, { label: string; tag: string }> = {
  1: { label: '充值', tag: 'success' },
  2: { label: '消费', tag: 'danger' },
  3: { label: '退款', tag: 'warning' },
  4: { label: '积分累计', tag: '' },
  5: { label: '积分抵扣', tag: 'info' },
  6: { label: '人工调整', tag: 'warning' }
}

// ===== 会员列表 =====
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  pageSize: 10,
  phone: '',
  level: undefined as number | undefined
})

const loadList = async () => {
  loading.value = true
  try {
    const { data: res } = await getMemberPageAPI(queryParams)
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
  queryParams.phone = ''
  queryParams.level = undefined
  handleSearch()
}
const handlePageChange = (p: number) => {
  queryParams.page = p
  loadList()
}

// ===== 新增办卡弹窗 =====
const addVisible = ref(false)
const addForm = reactive({ name: '', phone: '' })
const openAdd = () => {
  addForm.name = ''
  addForm.phone = ''
  addVisible.value = true
}
const submitAdd = async () => {
  if (!/^1\d{10}$/.test(addForm.phone)) return ElMessage.warning('请输入正确的11位手机号')
  const { data: res } = await addMemberAPI({ ...addForm })
  if (res.code === 0) {
    ElMessage.success('办卡成功，已自动成为银卡会员')
    addVisible.value = false
    loadList()
  }
}

// ===== 充值弹窗 =====
const rechargeVisible = ref(false)
const rechargeForm = reactive<any>({ memberId: undefined, memberName: '', amount: 200, gift: 20 })
// 快捷充值档（小白话术：充得多送得多）
const QUICK_RECHARGE = [
  { amount: 100, gift: 5 },
  { amount: 200, gift: 20 },
  { amount: 500, gift: 80 }
]
const openRecharge = (row: any) => {
  rechargeForm.memberId = row.id
  rechargeForm.memberName = row.name || row.phone
  rechargeForm.amount = 200
  rechargeForm.gift = 20
  rechargeVisible.value = true
}
const submitRecharge = async () => {
  if (!rechargeForm.amount || rechargeForm.amount <= 0) return ElMessage.warning('请输入充值金额')
  await ElMessageBox.confirm(
    `确认为「${rechargeForm.memberName}」充值 ${rechargeForm.amount} 元` +
      (Number(rechargeForm.gift) > 0 ? `（含赠送 ${rechargeForm.gift} 元）` : '') + '吗？',
    '充值确认',
    { confirmButtonText: '确认充值', cancelButtonText: '取消', type: 'info' }
  )
  const { data: res } = await rechargeMemberAPI({
    memberId: rechargeForm.memberId,
    amount: rechargeForm.amount,
    gift: Number(rechargeForm.gift) || 0
  })
  if (res.code === 0) {
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    loadList()
  }
}

// ===== 调整积分弹窗 =====
const pointsVisible = ref(false)
const pointsForm = reactive<any>({ memberId: undefined, memberName: '', points: 0, remark: '' })
const openPoints = (row: any) => {
  pointsForm.memberId = row.id
  pointsForm.memberName = row.name || row.phone
  pointsForm.points = 0
  pointsForm.remark = ''
  pointsVisible.value = true
}
const submitPoints = async () => {
  if (!pointsForm.points || pointsForm.points === 0) return ElMessage.warning('请输入要调整的积分（正数增加/负数扣减）')
  if (!pointsForm.remark.trim()) return ElMessage.warning('请填写调整原因，方便日后对账')
  const { data: res } = await adjustMemberPointsAPI({ ...pointsForm, remark: pointsForm.remark.trim() })
  if (res.code === 0) {
    ElMessage.success('积分调整成功')
    pointsVisible.value = false
    loadList()
  }
}

// ===== 流水抽屉 =====
const flowVisible = ref(false)
const flowLoading = ref(false)
const flowList = ref<any[]>([])
const flowTotal = ref(0)
const flowQuery = reactive({ memberId: 0, memberName: '', type: undefined as number | undefined, page: 1, pageSize: 8 })

const loadFlows = async () => {
  flowLoading.value = true
  try {
    const { data: res } = await getMemberFlowAPI(flowQuery.memberId, {
      page: flowQuery.page,
      pageSize: flowQuery.pageSize,
      type: flowQuery.type
    })
    if (res.code === 0) {
      flowList.value = res.data.records || []
      flowTotal.value = Number(res.data.total)
    }
  } finally {
    flowLoading.value = false
  }
}
const openFlows = (row: any) => {
  flowQuery.memberId = row.id
  flowQuery.memberName = row.name || row.phone
  flowQuery.type = undefined
  flowQuery.page = 1
  flowVisible.value = true
  loadFlows()
}
const handleFlowPageChange = (p: number) => {
  flowQuery.page = p
  loadFlows()
}

// ===== 等级规则弹窗 =====
const rulesVisible = ref(false)
const rulesList = ref<any[]>([])
const openRules = async () => {
  rulesVisible.value = true
  const { data: res } = await getMemberRulesAPI()
  if (res.code === 0) rulesList.value = (res.data || []).map((r: any) => ({ ...r }))
}
const submitRules = async () => {
  // 简单校验：门槛递增、折扣在0.1~1之间
  const sorted = [...rulesList.value].sort((a, b) => Number(a.thresholdAmount) - Number(b.thresholdAmount))
  for (const r of sorted) {
    if (Number(r.discount) < 0.1 || Number(r.discount) > 1) return ElMessage.warning(`「${r.levelName}」折扣需在0.1~1之间`)
  }
  const { data: res } = await updateMemberRulesAPI(sorted)
  if (res.code === 0) {
    ElMessage.success('等级规则已更新，后续下单自动按新规则算账')
    rulesVisible.value = false
    loadList()
  }
}

onMounted(loadList)
</script>

<template>
  <div class="member-page">
    <el-card class="page-card">
      <div class="page-head">
        <div class="page-head__meta">
          <h2>堂食会员</h2>
          <p>老顾客报手机号即可识别：余额支付、消费攒积分、等级自动升级，越吃越优惠。</p>
        </div>
        <div class="head-btns">
          <el-button plain @click="openRules">
            <el-icon style="margin-right: 4px"><Setting /></el-icon>等级规则
          </el-button>
          <el-button type="primary" @click="openAdd">
            <el-icon style="margin-right: 4px"><Plus /></el-icon>新增办卡
          </el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-input v-model="queryParams.phone" placeholder="手机号/姓名" clearable style="width: 200px"
          @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="queryParams.level" placeholder="会员等级" clearable style="width: 150px" @change="handleSearch">
          <el-option label="银卡会员" :value="1" />
          <el-option label="金卡会员" :value="2" />
          <el-option label="钻石会员" :value="3" />
        </el-select>
        <el-button type="primary" plain @click="handleSearch">查询</el-button>
        <el-button plain @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column label="手机号" prop="phone" width="130" />
        <el-table-column label="姓名" min-width="100">
          <template #default="{ row }">{{ row.name || '未登记' }}</template>
        </el-table-column>
        <el-table-column label="等级" min-width="150">
          <template #default="{ row }">
            <el-tag :type="(LEVEL_TAG[row.level] as any) || 'info'" effect="dark" round size="small">
              {{ row.levelName || '银卡会员' }}
            </el-tag>
            <span class="level-discount" v-if="row.discount && Number(row.discount) < 1">
              享{{ Number(row.discount) * 10 }}折
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余额(元)" width="110">
          <template #default="{ row }">
            <span class="money">￥{{ Number(row.balance).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="积分" width="90">
          <template #default="{ row }">
            <span class="points">{{ row.points }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计消费(元)" width="130">
          <template #default="{ row }">￥{{ Number(row.totalConsume).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="注册时间" prop="createTime" min-width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRecharge(row)">充值</el-button>
            <el-button link type="warning" @click="openPoints(row)">调积分</el-button>
            <el-button link type="info" @click="openFlows(row)">流水</el-button>
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

    <!-- 新增办卡 -->
    <el-dialog v-model="addVisible" title="新增会员办卡" width="420px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="手机号" required>
          <el-input v-model="addForm.phone" maxlength="11" placeholder="11位手机号，会员唯一标识" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.name" maxlength="20" placeholder="选填，方便称呼老顾客" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="提示：顾客首次堂食消费并留下手机号时，系统也会自动建档。" />
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认办卡</el-button>
      </template>
    </el-dialog>

    <!-- 充值 -->
    <el-dialog v-model="rechargeVisible" title="会员充值" width="460px">
      <el-form :model="rechargeForm" label-width="92px">
        <el-form-item label="会员">{{ rechargeForm.memberName }}</el-form-item>
        <el-form-item label="快捷充值">
          <el-radio-group
            :model-value="QUICK_RECHARGE.find(q => q.amount === rechargeForm.amount && q.gift === rechargeForm.gift)?.amount"
          >
            <el-radio-button
              v-for="q in QUICK_RECHARGE"
              :key="q.amount"
              :value="q.amount"
              @click="Object.assign(rechargeForm, q)"
            >充{{ q.amount }}送{{ q.gift }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="充值本金" required>
          <el-input-number v-model="rechargeForm.amount" :min="1" :precision="2" />
          <span class="form-hint">元</span>
        </el-form-item>
        <el-form-item label="赠送金额">
          <el-input-number v-model="rechargeForm.gift" :min="0" :precision="2" />
          <span class="form-hint">元（充200送20就填20）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- 调整积分 -->
    <el-dialog v-model="pointsVisible" title="人工调整积分" width="460px">
      <el-form :model="pointsForm" label-width="92px">
        <el-form-item label="会员">{{ pointsForm.memberName }}</el-form-item>
        <el-form-item label="调整数量" required>
          <el-input-number v-model="pointsForm.points" :step="100" />
          <span class="form-hint">正数为加积分，负数为扣积分</span>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="pointsForm.remark" maxlength="100" show-word-limit
            placeholder="如：顾客反馈菜品问题补偿100积分" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPoints">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 消费/积分流水抽屉 -->
    <el-drawer v-model="flowVisible" :title="`会员流水 - ${flowQuery.memberName}`" size="720px">
      <div class="flow-filter">
        <el-select v-model="flowQuery.type" placeholder="全部类型" clearable style="width: 150px" @change="handleFlowPageChange(1)">
          <el-option v-for="(item, key) in FLOW_TYPE_MAP" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
      </div>
      <el-table v-loading="flowLoading" :data="flowList" stripe size="small">
        <el-table-column label="时间" prop="createTime" width="165" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="(FLOW_TYPE_MAP[row.type]?.tag as any) || 'info'" size="small" round>
              {{ FLOW_TYPE_MAP[row.type]?.label || '其他' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额变动" width="110">
          <template #default="{ row }">
            <span v-if="row.amount" :class="Number(row.amount) >= 0 ? 'amt-in' : 'amt-out'">
              {{ Number(row.amount) >= 0 ? '+' : '' }}{{ Number(row.amount).toFixed(2) }}
            </span>
            <span v-else class="dash">-</span>
          </template>
        </el-table-column>
        <el-table-column label="积分变动" width="90">
          <template #default="{ row }">
            <span v-if="row.points" :class="Number(row.points) >= 0 ? 'amt-in' : 'amt-out'">
              {{ Number(row.points) >= 0 ? '+' : '' }}{{ row.points }}
            </span>
            <span v-else class="dash">-</span>
          </template>
        </el-table-column>
        <el-table-column label="变动后余额/积分" min-width="130">
          <template #default="{ row }">
            ￥{{ Number(row.balanceAfter).toFixed(2) }} / {{ row.pointsAfter }}分
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="flowTotal > flowQuery.pageSize"
        class="pager"
        small
        background
        layout="prev, pager, next, total"
        :total="flowTotal"
        :page-size="flowQuery.pageSize"
        :current-page="flowQuery.page"
        @current-change="handleFlowPageChange"
      />
    </el-drawer>

    <!-- 等级规则 -->
    <el-dialog v-model="rulesVisible" title="会员等级规则" width="560px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 14px"
        title="等级按累计消费自动升级（只能往上升不能降级）；折扣在营销活动优惠价基础上再打折，1=不打折，0.9=9折。" />
      <el-table :data="rulesList" border size="small">
        <el-table-column label="等级" min-width="110">
          <template #default="{ row }">
            <el-input v-model="row.levelName" />
          </template>
        </el-table-column>
        <el-table-column label="累计消费门槛(元)" min-width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.thresholdAmount" :min="0" :precision="2" :controls="false" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="折扣" min-width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.discount" :min="0.1" :max="1" :step="0.05" :precision="2"
              :controls="false" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="rulesVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRules">保存规则</el-button>
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

.head-btns {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin: 18px 0;
  flex-wrap: wrap;
}

.money {
  color: var(--brand-deep);
  font-weight: 700;
}

.points {
  color: #e6a23c;
  font-weight: 700;
}

.level-discount {
  margin-left: 8px;
  font-size: 0.8rem;
  color: var(--text-sub);
}

.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

.form-hint {
  margin-left: 10px;
  color: var(--text-sub);
  font-size: 0.85rem;
}

.flow-filter {
  margin-bottom: 14px;
}

.amt-in {
  color: #157347;
  font-weight: 700;
}

.amt-out {
  color: #dc3545;
  font-weight: 700;
}

.dash {
  color: #ccc;
}
</style>
