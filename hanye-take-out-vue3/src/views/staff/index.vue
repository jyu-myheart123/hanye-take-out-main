<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserInfoStore } from '@/store'
import {
  getStaffEmployeesAPI,
  submitStaffRatingAPI,
  staffTipAPI,
  getStaffRankAPI,
  getStaffRatingPageAPI,
  toggleStaffRatingStatusAPI,
  deleteStaffRatingAPI,
  getStaffRewardPageAPI,
  addStaffRewardAPI
} from '@/api/staff'

// 登录人信息：只有管理员 role=1 才能看到"奖励/惩戒"按钮（后端也会再校验一次）
const userInfoStore = useUserInfoStore()
const isAdmin = () => userInfoStore.userInfo?.role === 1

const activeTab = ref('rating')
// 在职员工列表（评分选择、记录筛选都要用）
const employeeList = ref<any[]>([])

/** 加载在职员工列表 */
const loadEmployees = async () => {
  const { data: res } = await getStaffEmployeesAPI()
  if (res.code === 0) employeeList.value = res.data || []
}

// ============================================================
// Tab1：服务评分问卷（员工照着顾客反馈代填）
// ============================================================
const ratingForm = reactive({
  employeeId: undefined as number | undefined,
  tableNo: '',
  customerName: '',
  serviceScore: 5, // 服务态度，必填，默认5星
  recommendScore: 0, // 推荐指数，0=顾客没打这一项
  content: ''
})
const submitting = ref(false)

/** 提交评分；成功后接着弹"是否给小费" */
const submitRating = async () => {
  if (!ratingForm.employeeId) return ElMessage.warning('请选择提供服务的员工')
  if (!ratingForm.serviceScore) return ElMessage.warning('请给服务态度打分')
  submitting.value = true
  try {
    const { data: res } = await submitStaffRatingAPI({
      employeeId: ratingForm.employeeId,
      tableNo: ratingForm.tableNo.trim() || null,
      customerName: ratingForm.customerName.trim() || null,
      serviceScore: ratingForm.serviceScore,
      recommendScore: ratingForm.recommendScore || null,
      content: ratingForm.content.trim() || null
    })
    if (res.code === 0) {
      // 评分先入库成功，再询问顾客要不要给小费
      tipRatingId.value = res.data.id
      tipForm.amount = 5
      tipVisible.value = true
    }
  } finally {
    submitting.value = false
  }
}

/** 评分提交后清空问卷（员工保留，方便连续为同一员工录分） */
const resetRatingForm = () => {
  ratingForm.tableNo = ''
  ratingForm.customerName = ''
  ratingForm.serviceScore = 5
  ratingForm.recommendScore = 0
  ratingForm.content = ''
}

// ----- 打赏小费弹窗 -----
const tipVisible = ref(false)
const tipSaving = ref(false)
const tipRatingId = ref<number>()
const tipForm = reactive({ amount: 5 })
// 快捷小费金额按钮
const quickTips = [2, 5, 10, 20]

/** 确认给小费 */
const confirmTip = async () => {
  if (!tipForm.amount || Number(tipForm.amount) <= 0) {
    return ElMessage.warning('请输入大于0的小费金额')
  }
  tipSaving.value = true
  try {
    const { data: res } = await staffTipAPI({
      ratingId: tipRatingId.value!,
      amount: Number(tipForm.amount)
    })
    if (res.code === 0) {
      ElMessage.success(`已收到 ￥${Number(tipForm.amount).toFixed(2)} 小费，感谢顾客的打赏！`)
      tipVisible.value = false
      resetRatingForm()
      // 如果之前已经加载过排行榜/记录，顺带刷新一下数字
      if (rankLoaded.value) loadRank()
      if (ratingLoaded.value) loadRatingPage()
      if (rewardLoaded.value) loadRewardPage()
    }
  } finally {
    tipSaving.value = false
  }
}

/** 顾客选择不给小费 */
const skipTip = () => {
  tipVisible.value = false
  ElMessage.success('评分提交成功，感谢顾客的反馈！')
  resetRatingForm()
  if (rankLoaded.value) loadRank()
  if (ratingLoaded.value) loadRatingPage()
}

// ============================================================
// Tab2：北极星排行榜（今日 / 近3日 / 近7日 / 近一月）
// ============================================================
const rankRange = ref('today')
const rangeOptions = [
  { label: '今日', value: 'today' },
  { label: '近3日', value: '3d' },
  { label: '近7日', value: '7d' },
  { label: '近一月', value: 'month' }
]
const rankLoading = ref(false)
const rankList = ref<any[]>([])
const rankLoaded = ref(false)

/** 拉取排行榜 */
const loadRank = async () => {
  rankLoading.value = true
  rankLoaded.value = true
  try {
    const { data: res } = await getStaffRankAPI(rankRange.value)
    if (res.code === 0) rankList.value = res.data || []
  } finally {
    rankLoading.value = false
  }
}

/** 切换时间范围 */
const onRangeChange = () => loadRank()

// ----- 店主奖励/惩戒弹窗 -----
const rewardVisible = ref(false)
const rewardSaving = ref(false)
const rewardForm = reactive({
  employeeId: undefined as number | undefined,
  employeeName: '',
  mode: 'reward' as 'reward' | 'punish',
  amount: 0,
  starPoints: 0,
  reason: ''
})

/** 打开奖惩弹窗（mode: reward=奖励 punish=惩戒，row 是排行榜那一行） */
const openReward = (row: any, mode: 'reward' | 'punish') => {
  rewardForm.employeeId = row.employeeId
  rewardForm.employeeName = row.employeeName
  rewardForm.mode = mode
  rewardForm.amount = 0
  rewardForm.starPoints = 0
  rewardForm.reason = ''
  rewardVisible.value = true
}

/** 提交奖惩 */
const submitReward = async () => {
  if (rewardForm.amount < 0 || rewardForm.starPoints < 0) {
    return ElMessage.warning('金额和积分请填正数，系统会按奖励/惩戒自动记正负')
  }
  if (Number(rewardForm.amount) === 0 && Number(rewardForm.starPoints) === 0) {
    return ElMessage.warning('金额和北极星积分不能都为0')
  }
  if (!rewardForm.reason.trim()) return ElMessage.warning('请填写奖惩原因，方便后续核对')
  rewardSaving.value = true
  try {
    const { data: res } = await addStaffRewardAPI({
      employeeId: rewardForm.employeeId,
      type: rewardForm.mode === 'reward' ? 2 : 3,
      amount: Number(rewardForm.amount),
      starPoints: Number(rewardForm.starPoints),
      reason: rewardForm.reason.trim()
    })
    if (res.code === 0) {
      ElMessage.success(rewardForm.mode === 'reward' ? '奖励已发放' : '惩戒已记录')
      rewardVisible.value = false
      loadRank()
      if (rewardLoaded.value) loadRewardPage()
    }
  } finally {
    rewardSaving.value = false
  }
}

// ============================================================
// Tab3：评分记录
// ============================================================
const ratingLoading = ref(false)
const ratingList = ref<any[]>([])
const ratingTotal = ref(0)
const ratingLoaded = ref(false)
const ratingQuery = reactive({
  page: 1,
  pageSize: 10,
  employeeId: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: ''
})

/** 评分记录分页查询 */
const loadRatingPage = async () => {
  ratingLoading.value = true
  ratingLoaded.value = true
  try {
    const { data: res } = await getStaffRatingPageAPI(ratingQuery)
    if (res.code === 0) {
      ratingList.value = res.data.records || []
      ratingTotal.value = Number(res.data.total)
    }
  } finally {
    ratingLoading.value = false
  }
}
const ratingSearch = () => {
  ratingQuery.page = 1
  loadRatingPage()
}
const ratingPageChange = (p: number) => {
  ratingQuery.page = p
  loadRatingPage()
}
/** 显示/隐藏（隐藏后不计入排行榜） */
const toggleRatingStatus = async (row: any) => {
  const { data: res } = await toggleStaffRatingStatusAPI(row.id)
  if (res.code === 0) {
    ElMessage.success(row.status === 1 ? '已隐藏，该条不计入排名' : '已恢复显示')
    loadRatingPage()
    loadRank()
  }
}
/** 删除评分 */
const removeRating = (row: any) => {
  ElMessageBox.confirm(`确定删除 ${row.employeeName} 的这条服务评分吗？`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      const { data: res } = await deleteStaffRatingAPI(row.id)
      if (res.code === 0) {
        ElMessage.success('已删除')
        loadRatingPage()
        loadRank()
      }
    })
    .catch(() => {})
}

// ============================================================
// Tab4：赏罚流水
// ============================================================
const rewardLoading = ref(false)
const rewardList = ref<any[]>([])
const rewardTotal = ref(0)
const rewardLoaded = ref(false)
const rewardQuery = reactive({
  page: 1,
  pageSize: 10,
  employeeId: undefined as number | undefined,
  type: undefined as number | undefined
})
// 流水类型展示：1顾客打赏 2店主奖励 3店主惩戒
const rewardTypeMap: Record<number, { text: string; tag: string }> = {
  1: { text: '顾客打赏', tag: 'success' },
  2: { text: '店主奖励', tag: 'warning' },
  3: { text: '店主惩戒', tag: 'danger' }
}

/** 赏罚流水分页查询 */
const loadRewardPage = async () => {
  rewardLoading.value = true
  rewardLoaded.value = true
  try {
    const { data: res } = await getStaffRewardPageAPI(rewardQuery)
    if (res.code === 0) {
      rewardList.value = res.data.records || []
      rewardTotal.value = Number(res.data.total)
    }
  } finally {
    rewardLoading.value = false
  }
}
const rewardSearch = () => {
  rewardQuery.page = 1
  loadRewardPage()
}
const rewardPageChange = (p: number) => {
  rewardQuery.page = p
  loadRewardPage()
}

/** 切换标签时懒加载对应数据（第一次点进去才请求） */
const onTabChange = (name: string | number) => {
  if (name === 'rank' && !rankLoaded.value) loadRank()
  if (name === 'records' && !ratingLoaded.value) loadRatingPage()
  if (name === 'flows' && !rewardLoaded.value) loadRewardPage()
}

onMounted(() => {
  loadEmployees()
})
</script>

<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <!-- ================= Tab1：服务评分 ================= -->
      <el-tab-pane label="服务评分" name="rating">
        <el-card shadow="never" class="rating-card">
          <template #header>
            <span class="card-title">顾客服务评分问卷</span>
          </template>
          <el-form label-position="top" style="max-width: 560px">
            <el-form-item label="服务员工（必选）">
              <el-select
                v-model="ratingForm.employeeId"
                placeholder="请选择本次为顾客服务的员工"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="emp in employeeList"
                  :key="emp.id"
                  :label="emp.role === 1 ? `${emp.name}（管理员）` : emp.name"
                  :value="emp.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="桌号（选填）">
              <el-input v-model="ratingForm.tableNo" placeholder="如：A3桌" maxlength="32" />
            </el-form-item>

            <el-form-item label="顾客称呼（选填）">
              <el-input v-model="ratingForm.customerName" placeholder="如：王先生" maxlength="32" />
            </el-form-item>

            <el-form-item label="服务态度（必填，几星就得几个北极星积分）">
              <el-rate
                v-model="ratingForm.serviceScore"
                :texts="['很不满意', '不满意', '一般', '满意', '非常满意']"
                show-text
              />
            </el-form-item>

            <el-form-item label="推荐指数：员工推荐的菜品合不合心意（选填）">
              <el-rate
                v-model="ratingForm.recommendScore"
                :texts="['完全没推荐对', '不太合心意', '一般', '比较合心意', '推荐得太准了']"
                show-text
                clearable
              />
            </el-form-item>

            <el-form-item label="评价内容（选填）">
              <el-input
                v-model="ratingForm.content"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="顾客有什么想说的，都可以记在这里"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="submitRating"
              >
                提交评分
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ================= Tab2：星光排行榜 ================= -->
      <el-tab-pane label="北极星排行榜" name="rank">
        <div class="rank-toolbar">
          <el-radio-group v-model="rankRange" @change="onRangeChange">
            <el-radio-button
              v-for="opt in rangeOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
          <span class="rank-tip">按平均分从高到低排名，可作为每周奖金结算的参考</span>
        </div>

        <el-table v-loading="rankLoading" :data="rankList" stripe>
          <el-table-column label="排名" width="90" align="center">
            <template #default="{ $index }">
              <span v-if="$index === 0" class="medal gold">🥇 第1</span>
              <span v-else-if="$index === 1" class="medal silver">🥈 第2</span>
              <span v-else-if="$index === 2" class="medal bronze">🥉 第3</span>
              <span v-else class="rank-normal">第{{ $index + 1 }}名</span>
            </template>
          </el-table-column>
          <el-table-column label="员工" prop="employeeName" width="120" />
          <el-table-column label="评价人数" prop="ratingCount" width="100" align="center" />
          <el-table-column label="平均服务分" width="130" align="center">
            <template #default="{ row }">
              <span class="big-score">{{ Number(row.avgService).toFixed(2) }}</span>
              <span class="score-full"> / 5</span>
            </template>
          </el-table-column>
          <el-table-column label="平均推荐指数" width="130" align="center">
            <template #default="{ row }">
              <el-rate
                :model-value="Number(row.avgRecommend)"
                disabled
                allow-half
                :show-text="false"
              />
            </template>
          </el-table-column>
          <el-table-column label="北极星积分" width="130" align="center">
            <template #default="{ row }">
              <el-tag type="warning" effect="dark" round>⭐ {{ row.starPoints }} 分</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="收到小费" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.tipTotal).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="店主奖惩净额" width="130" align="right">
            <template #default="{ row }">
              <span :class="Number(row.rewardNet) < 0 ? 'money-minus' : 'money-plus'">
                ￥{{ Number(row.rewardNet).toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <template v-if="isAdmin()">
                <el-button link type="warning" @click="openReward(row, 'reward')">奖励</el-button>
                <el-button link type="danger" @click="openReward(row, 'punish')">惩戒</el-button>
              </template>
              <span v-else class="no-perm">仅管理员可操作</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="这段时间还没有员工收到服务评分" />
          </template>
        </el-table>
      </el-tab-pane>

      <!-- ================= Tab3：评分记录 ================= -->
      <el-tab-pane label="评分记录" name="records">
        <div class="filter-bar">
          <el-select
            v-model="ratingQuery.employeeId"
            placeholder="按员工筛选"
            clearable
            style="width: 170px"
            @change="ratingSearch"
          >
            <el-option
              v-for="emp in employeeList"
              :key="emp.id"
              :label="emp.name"
              :value="emp.id"
            />
          </el-select>
          <el-select
            v-model="ratingQuery.status"
            placeholder="按状态筛选"
            clearable
            style="width: 140px"
            @change="ratingSearch"
          >
            <el-option label="有效" :value="1" />
            <el-option label="已隐藏" :value="0" />
          </el-select>
          <el-input
            v-model="ratingQuery.keyword"
            placeholder="搜评语 / 顾客 / 桌号 / 员工"
            clearable
            style="width: 240px"
            @keyup.enter="ratingSearch"
            @clear="ratingSearch"
          />
          <el-button type="primary" @click="ratingSearch">查询</el-button>
        </div>

        <el-table v-loading="ratingLoading" :data="ratingList" stripe>
          <el-table-column label="时间" prop="createTime" width="165" />
          <el-table-column label="员工" prop="employeeName" width="100" />
          <el-table-column label="桌号/顾客" width="130">
            <template #default="{ row }">
              {{ row.tableNo || '—' }}<template v-if="row.customerName"> / {{ row.customerName }}</template>
            </template>
          </el-table-column>
          <el-table-column label="服务态度" width="150">
            <template #default="{ row }">
              <el-rate :model-value="row.serviceScore" disabled :show-text="false" />
            </template>
          </el-table-column>
          <el-table-column label="推荐指数" width="150">
            <template #default="{ row }">
              <el-rate
                :model-value="row.recommendScore || 0"
                disabled
                :show-text="false"
              />
              <span v-if="!row.recommendScore" class="muted">未评</span>
            </template>
          </el-table-column>
          <el-table-column label="评价内容" prop="content" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content || '—' }}</template>
          </el-table-column>
          <el-table-column label="小费" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.tipAmount).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="北极星" width="80" align="center">
            <template #default="{ row }">+{{ row.starPoints }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" round>
                {{ row.status === 1 ? '有效' : '隐藏' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link :type="row.status === 1 ? 'info' : 'success'" @click="toggleRatingStatus(row)">
                {{ row.status === 1 ? '隐藏' : '恢复' }}
              </el-button>
              <el-button link type="danger" @click="removeRating(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pager"
          layout="total, prev, pager, next"
          :total="ratingTotal"
          :page-size="ratingQuery.pageSize"
          :current-page="ratingQuery.page"
          @current-change="ratingPageChange"
        />
      </el-tab-pane>

      <!-- ================= Tab4：赏罚流水 ================= -->
      <el-tab-pane label="赏罚流水" name="flows">
        <div class="filter-bar">
          <el-select
            v-model="rewardQuery.employeeId"
            placeholder="按员工筛选"
            clearable
            style="width: 170px"
            @change="rewardSearch"
          >
            <el-option
              v-for="emp in employeeList"
              :key="emp.id"
              :label="emp.name"
              :value="emp.id"
            />
          </el-select>
          <el-select
            v-model="rewardQuery.type"
            placeholder="按类型筛选"
            clearable
            style="width: 150px"
            @change="rewardSearch"
          >
            <el-option label="顾客打赏" :value="1" />
            <el-option label="店主奖励" :value="2" />
            <el-option label="店主惩戒" :value="3" />
          </el-select>
          <el-button type="primary" @click="rewardSearch">查询</el-button>
        </div>

        <el-table v-loading="rewardLoading" :data="rewardList" stripe>
          <el-table-column label="时间" prop="createTime" width="165" />
          <el-table-column label="员工" prop="employeeName" width="120" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag
                :type="(rewardTypeMap[row.type]?.tag as any) || 'info'"
                size="small"
                round
              >
                {{ rewardTypeMap[row.type]?.text || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="130" align="right">
            <template #default="{ row }">
              <span :class="Number(row.amount) < 0 ? 'money-minus' : 'money-plus'">
                {{ Number(row.amount) < 0 ? '-' : '+' }}￥{{ Math.abs(Number(row.amount)).toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="北极星积分" width="120" align="center">
            <template #default="{ row }">
              <span v-if="row.starPoints !== 0" :class="row.starPoints < 0 ? 'money-minus' : 'money-plus'">
                {{ row.starPoints > 0 ? '+' : '' }}{{ row.starPoints }}
              </span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="原因/说明" prop="reason" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.reason || '—' }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pager"
          layout="total, prev, pager, next"
          :total="rewardTotal"
          :page-size="rewardQuery.pageSize"
          :current-page="rewardQuery.page"
          @current-change="rewardPageChange"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 评分成功后的"是否给小费"弹窗 ===== -->
    <el-dialog v-model="tipVisible" title="感谢顾客的好评！" width="400px" :close-on-click-modal="false">
      <div class="tip-body">
        <p class="tip-question">顾客是否要给服务员工一点小费打赏？</p>
        <div class="quick-tips">
          <el-button
            v-for="m in quickTips"
            :key="m"
            :type="Number(tipForm.amount) === m ? 'primary' : 'default'"
            round
            @click="tipForm.amount = m"
          >
            ￥{{ m }}
          </el-button>
        </div>
        <el-input-number
          v-model="tipForm.amount"
          :min="0"
          :precision="2"
          :step="1"
          controls-position="right"
          style="width: 100%"
        />
      </div>
      <template #footer>
        <el-button @click="skipTip">不用了，谢谢</el-button>
        <el-button type="primary" :loading="tipSaving" @click="confirmTip">
          确认给小费
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 店主奖励/惩戒弹窗 ===== -->
    <el-dialog
      v-model="rewardVisible"
      :title="rewardForm.mode === 'reward' ? `奖励员工：${rewardForm.employeeName}` : `惩戒员工：${rewardForm.employeeName}`"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="金额（元，奖励为奖金 / 惩戒为扣款）">
          <el-input-number
            v-model="rewardForm.amount"
            :min="0"
            :precision="2"
            :step="10"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="北极星积分变动（可只动钱不调积分）">
          <el-input-number
            v-model="rewardForm.starPoints"
            :min="0"
            :step="5"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原因（必填）">
          <el-input
            v-model="rewardForm.reason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            :placeholder="rewardForm.mode === 'reward' ? '如：本周服务评分第一' : '如：多次被顾客投诉服务态度'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rewardVisible = false">取消</el-button>
        <el-button
          :type="rewardForm.mode === 'reward' ? 'warning' : 'danger'"
          :loading="rewardSaving"
          @click="submitReward"
        >
          确认{{ rewardForm.mode === 'reward' ? '奖励' : '惩戒' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-title {
  font-weight: 600;
  font-size: 16px;
}
.rank-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}
.rank-tip {
  color: #909399;
  font-size: 13px;
}
.big-score {
  font-size: 18px;
  font-weight: 700;
  color: #e6a23c;
}
.score-full {
  color: #c0c4cc;
  font-size: 12px;
}
.medal {
  font-weight: 700;
}
.gold {
  color: #e6a23c;
}
.silver {
  color: #909399;
}
.bronze {
  color: #cd7f32;
}
.rank-normal {
  color: #606266;
}
.money-plus {
  color: #67c23a;
  font-weight: 600;
}
.money-minus {
  color: #f56c6c;
  font-weight: 600;
}
.muted {
  color: #c0c4cc;
  font-size: 12px;
  margin-left: 6px;
}
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.no-perm {
  color: #c0c4cc;
  font-size: 12px;
}
.tip-body {
  text-align: center;
}
.tip-question {
  margin: 0 0 16px;
  font-size: 15px;
  color: #303133;
}
.quick-tips {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 16px;
}
.rating-card {
  max-width: 720px;
}
</style>
