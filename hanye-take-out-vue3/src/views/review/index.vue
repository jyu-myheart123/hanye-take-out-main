<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPendingReviewPageAPI,
  getReviewPageAPI,
  submitReviewAPI,
  replyReviewAPI,
  toggleReviewStatusAPI,
  deleteReviewAPI
} from '@/api/review'

const activeTab = ref('pending')

// ===== Tab1：待评价订单 =====
const pendingLoading = ref(false)
const pendingList = ref<any[]>([])
const pendingTotal = ref(0)
const pendingQuery = reactive({ page: 1, pageSize: 10, phone: '' })

const loadPending = async () => {
  pendingLoading.value = true
  try {
    const { data: res } = await getPendingReviewPageAPI(pendingQuery)
    if (res.code === 0) {
      pendingList.value = res.data.records || []
      pendingTotal.value = Number(res.data.total)
    }
  } finally {
    pendingLoading.value = false
  }
}
const pendingSearch = () => {
  pendingQuery.page = 1
  loadPending()
}
const pendingPageChange = (p: number) => {
  pendingQuery.page = p
  loadPending()
}

// ===== 评价填写弹窗 =====
const reviewVisible = ref(false)
const reviewSaving = ref(false)
const reviewForm = reactive<any>({
  orderId: undefined,
  tableNo: '',
  orderDishes: '',
  amount: 0,
  customerName: '',
  dishScore: 5,
  tasteScore: undefined as number | undefined,
  serviceScore: 5,
  speedScore: undefined as number | undefined,
  content: '',
  suggestion: ''
})

/** 打开评价弹窗（把订单信息带进来，方便照着顾客反馈填写） */
const openReview = (row: any) => {
  Object.assign(reviewForm, {
    orderId: row.id,
    tableNo: row.consignee || '堂食',
    orderDishes: row.orderDishes || '',
    amount: row.amount,
    customerName: '',
    dishScore: 5,
    tasteScore: undefined,
    serviceScore: 5,
    speedScore: undefined,
    content: '',
    suggestion: ''
  })
  reviewVisible.value = true
}

/** 提交评价（菜品、服务态度为必填星） */
const submitReview = async () => {
  if (!reviewForm.dishScore) return ElMessage.warning('请给菜品总体打分')
  if (!reviewForm.serviceScore) return ElMessage.warning('请给服务态度打分')
  if (!reviewForm.content?.trim() && !reviewForm.suggestion?.trim()) {
    return ElMessage.warning('好评内容和改进意见至少填写一项哦')
  }
  reviewSaving.value = true
  try {
    const { data: res } = await submitReviewAPI({
      orderId: reviewForm.orderId,
      customerName: reviewForm.customerName || null,
      dishScore: reviewForm.dishScore,
      tasteScore: reviewForm.tasteScore || null,
      serviceScore: reviewForm.serviceScore,
      speedScore: reviewForm.speedScore || null,
      content: reviewForm.content || null,
      suggestion: reviewForm.suggestion || null
    })
    if (res.code === 0) {
      ElMessage.success('评价提交成功，感谢顾客的反馈！')
      reviewVisible.value = false
      loadPending()
    }
  } finally {
    reviewSaving.value = false
  }
}

// ===== Tab2：评价列表 =====
const listLoading = ref(false)
const reviewList = ref<any[]>([])
const reviewTotal = ref(0)
const listQuery = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })

const loadReviews = async () => {
  listLoading.value = true
  try {
    const { data: res } = await getReviewPageAPI(listQuery)
    if (res.code === 0) {
      reviewList.value = res.data.records || []
      reviewTotal.value = Number(res.data.total)
    }
  } finally {
    listLoading.value = false
  }
}
const listSearch = () => {
  listQuery.page = 1
  loadReviews()
}
const listPageChange = (p: number) => {
  listQuery.page = p
  loadReviews()
}
const tabChange = (tab: string) => {
  if (tab === 'list' && reviewList.value.length === 0) loadReviews()
}

/** 商家回复（弹窗输入多行文本） */
const handleReply = (row: any) => {
  ElMessageBox.prompt('请输入回复内容（会展示在评价下方）', `回复「${row.tableNo}」顾客`, {
    confirmButtonText: '发送回复',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: row.reply || '',
    inputValidator: (v: string) => (v && v.trim() ? true : '回复内容不能为空')
  })
    .then(async ({ value }) => {
      const { data: res } = await replyReviewAPI({ id: row.id, reply: value.trim() })
      if (res.code === 0) {
        ElMessage.success('回复成功')
        loadReviews()
      }
    })
    .catch(() => {})
}

/** 显示/隐藏不当评价 */
const handleToggle = async (row: any) => {
  const { data: res } = await toggleReviewStatusAPI(row.id)
  if (res.code === 0) {
    ElMessage.success(row.status === 1 ? '评价已隐藏' : '评价已展示')
    loadReviews()
  }
}

/** 删除评价（二次确认） */
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定删除这条评价吗？删除后无法恢复。', '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      const { data: res } = await deleteReviewAPI(row.id)
      if (res.code === 0) {
        ElMessage.success('删除成功')
        loadReviews()
      }
    })
    .catch(() => {})
}

/** 综合平均分：已打分项的平均值，保留1位 */
const avgScore = (row: any) => {
  const scores = [row.dishScore, row.tasteScore, row.serviceScore, row.speedScore].filter(s => s != null)
  if (!scores.length) return 0
  return Math.round((scores.reduce((a: number, b: number) => a + b, 0) / scores.length) * 10) / 10
}

/** 隐藏评价的行整行置灰，提醒管理者这条不对顾客展示 */
const rowStatusClass = ({ row }: { row: any }) => (row.status === 0 ? 'row-hidden' : '')

onMounted(loadPending)
</script>

<template>
  <div class="review-page">
    <el-card class="page-card">
      <div class="page-head">
        <h2>客户评价</h2>
        <p>只有「已完成的堂食订单」可以评价，一单一评；后厨/管理者可在评价列表回复与管理。</p>
      </div>

      <el-tabs v-model="activeTab" @tab-change="tabChange">
        <!-- ====== 待评价订单 ====== -->
        <el-tab-pane label="待评价订单" name="pending">
          <div class="filter-bar">
            <el-input v-model="pendingQuery.phone" placeholder="手机号/桌号搜索" clearable style="width: 220px"
              @keyup.enter="pendingSearch" @clear="pendingSearch" />
            <el-button type="primary" plain @click="pendingSearch">查询</el-button>
          </div>

          <el-table v-loading="pendingLoading" :data="pendingList" stripe>
            <el-table-column label="订单号" prop="number" min-width="170" show-overflow-tooltip />
            <el-table-column label="桌号/称呼" prop="consignee" width="110" />
            <el-table-column label="菜品明细" prop="orderDishes" min-width="220" show-overflow-tooltip />
            <el-table-column label="实付(元)" width="100">
              <template #default="{ row }">
                <span class="money">￥{{ Number(row.amount).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="完成时间" prop="deliveryTime" min-width="170" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openReview(row)">去评价</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="pendingTotal > pendingQuery.pageSize"
            class="pager"
            background
            layout="prev, pager, next, total"
            :total="pendingTotal"
            :page-size="pendingQuery.pageSize"
            :current-page="pendingQuery.page"
            @current-change="pendingPageChange"
          />
          <el-empty v-if="!pendingLoading && pendingList.length === 0" description="暂无已完成待评价的堂食订单" />
        </el-tab-pane>

        <!-- ====== 评价列表 ====== -->
        <el-tab-pane label="评价列表" name="list">
          <div class="filter-bar">
            <el-input v-model="listQuery.keyword" placeholder="桌号/评价人/内容关键词" clearable style="width: 240px"
              @keyup.enter="listSearch" @clear="listSearch" />
            <el-select v-model="listQuery.status" placeholder="状态" clearable style="width: 130px" @change="listSearch">
              <el-option label="展示中" :value="1" />
              <el-option label="已隐藏" :value="0" />
            </el-select>
            <el-button type="primary" plain @click="listSearch">查询</el-button>
          </div>

          <el-table v-loading="listLoading" :data="reviewList" stripe :row-class-name="rowStatusClass">
            <el-table-column label="桌号/评价人" min-width="130">
              <template #default="{ row }">
                <div class="who">
                  <strong>{{ row.tableNo || '堂食' }}</strong>
                  <span>{{ row.customerName || '匿名顾客' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="评分明细" min-width="190">
              <template #default="{ row }">
                <div class="scores">
                  <div><span>菜品</span><el-rate :model-value="row.dishScore" disabled size="small" /></div>
                  <div v-if="row.tasteScore"><span>口味</span><el-rate :model-value="row.tasteScore" disabled size="small" /></div>
                  <div><span>服务</span><el-rate :model-value="row.serviceScore" disabled size="small" /></div>
                  <div v-if="row.speedScore"><span>出餐</span><el-rate :model-value="row.speedScore" disabled size="small" /></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="综合" width="80">
              <template #default="{ row }">
                <em class="avg">{{ avgScore(row) }}</em>
              </template>
            </el-table-column>
            <el-table-column label="好评内容" prop="content" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.content || '-' }}</template>
            </el-table-column>
            <el-table-column label="改进意见" prop="suggestion" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span :class="{ suggestion: !!row.suggestion }">{{ row.suggestion || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="商家回复" min-width="170">
              <template #default="{ row }">
                <span v-if="row.reply" class="reply-text">{{ row.reply }}</span>
                <el-button v-else link type="primary" @click="handleReply(row)">去回复</el-button>
                <el-button v-if="row.reply" link type="info" @click="handleReply(row)">修改回复</el-button>
              </template>
            </el-table-column>
            <el-table-column label="菜品" prop="orderDishes" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch :model-value="row.status === 1" @change="handleToggle(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="reviewTotal > listQuery.pageSize"
            class="pager"
            background
            layout="prev, pager, next, total"
            :total="reviewTotal"
            :page-size="listQuery.pageSize"
            :current-page="listQuery.page"
            @current-change="listPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 评价填写弹窗 -->
    <el-dialog v-model="reviewVisible" title="顾客评价（员工代填）" width="560px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>
          <div class="order-info">
            <span>桌号：{{ reviewForm.tableNo }}</span>
            <span>实付：￥{{ Number(reviewForm.amount).toFixed(2) }}</span>
          </div>
          <div class="order-dishes">菜品：{{ reviewForm.orderDishes || '-' }}</div>
        </template>
      </el-alert>

      <el-form :model="reviewForm" label-width="92px">
        <el-form-item label="顾客称呼">
          <el-input v-model="reviewForm.customerName" maxlength="20" placeholder="选填，如：王先生 / 3号桌" />
        </el-form-item>

        <el-form-item label="菜品总体" required>
          <el-rate v-model="reviewForm.dishScore" show-text :texts="['很不满意','不满意','一般','满意','非常满意']" />
        </el-form-item>
        <el-form-item label="口味">
          <el-rate v-model="reviewForm.tasteScore" show-text :texts="['很不满意','不满意','一般','满意','非常满意']" />
        </el-form-item>
        <el-form-item label="服务态度" required>
          <el-rate v-model="reviewForm.serviceScore" show-text :texts="['很不满意','不满意','一般','满意','非常满意']" />
        </el-form-item>
        <el-form-item label="出餐速度">
          <el-rate v-model="reviewForm.speedScore" show-text :texts="['很慢','偏慢','一般','较快','很快']" />
        </el-form-item>

        <el-form-item label="好评内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="3" maxlength="300" show-word-limit
            placeholder="顾客觉得满意的地方，如：宫保鸡丁很入味" />
        </el-form-item>
        <el-form-item label="改进意见">
          <el-input v-model="reviewForm.suggestion" type="textarea" :rows="3" maxlength="300" show-word-limit
            placeholder="顾客希望改进的地方，如：希望米饭可以免费续" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSaving" @click="submitReview">提交评价</el-button>
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
  h2 {
    margin: 0 0 6px;
  }

  p {
    margin: 0 0 8px;
    color: var(--text-sub);
    font-size: 0.9rem;
  }
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin: 14px 0 18px;
  flex-wrap: wrap;
}

.money {
  color: var(--brand-deep);
  font-weight: 700;
}

.who {
  display: flex;
  flex-direction: column;
  gap: 2px;

  span {
    font-size: 0.78rem;
    color: var(--text-sub);
  }
}

.scores {
  display: flex;
  flex-direction: column;
  gap: 2px;

  > div {
    display: flex;
    align-items: center;
    gap: 6px;

    span {
      font-size: 0.76rem;
      color: var(--text-sub);
      width: 28px;
    }
  }
}

.avg {
  font-style: normal;
  font-weight: 800;
  font-size: 1.05rem;
  color: #e6a23c;
}

.suggestion {
  color: #b88230;
}

.reply-text {
  font-size: 0.85rem;
  color: #157347;
}

.order-info {
  display: flex;
  gap: 20px;
  font-weight: 600;
}

.order-dishes {
  margin-top: 4px;
  font-size: 0.82rem;
}

.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

// 隐藏评价整行置灰，提醒管理者这条不对顾客展示
:deep(.row-hidden) {
  opacity: 0.55;
}
</style>
