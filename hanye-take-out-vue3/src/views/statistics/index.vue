<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TurnoverStatistics from './components/TurnoverStatistics.vue'
import UserStatistics from './components/UserStatistics.vue'
import OrderStatistics from './components/OrderStatistics.vue'
import Top from './components/Top10.vue'
import { get1stAndToday, past7Day, past30Day, pastWeek, pastMonth } from '@/utils/date'
import {
  exportInforAPI,
  getOrderStatisticsAPI,
  getTop10StatisticsAPI,
  getTurnoverStatisticsAPI,
  getUserStatisticsAPI,
} from '@/api/statistics'

interface TurnoverData {
  dateList: string[]
  turnoverList: number[]
}

interface UserData {
  dateList: string[]
  totalUserList: number[]
  newUserList: number[]
}

interface OrderData {
  orderCompletionRate: number
  validOrderCount: number
  totalOrderCount: number
  data: {
    dateList: string[]
    orderCountList: number[]
    validOrderCountList: number[]
  }
}

interface Top10Data {
  nameList: string[]
  numberList: number[]
}

const overviewData = ref({})
const tateData = ref<string[]>([])
const turnoverData = ref<TurnoverData>({ dateList: [], turnoverList: [] })
const userData = ref<UserData>({ dateList: [], totalUserList: [], newUserList: [] })
const orderData = ref<OrderData>({
  orderCompletionRate: 0,
  validOrderCount: 0,
  totalOrderCount: 0,
  data: { dateList: [], orderCountList: [], validOrderCountList: [] },
})
const top10Data = ref<Top10Data>({ nameList: [], numberList: [] })
const nowIndex = ref(1)
const tabsParam = ['昨日', '近 7 日', '近 30 日', '本周', '本月']

const init = (begin: string, end: string) => {
  getTurnoverStatisticsData(begin, end)
  getUserStatisticsData(begin, end)
  getOrderStatisticsData(begin, end)
  getTopData(begin, end)
}

const getTurnoverStatisticsData = async (begin: string, end: string) => {
  const { data } = await getTurnoverStatisticsAPI({ begin, end })
  turnoverData.value = {
    dateList: data.data.dateList.split(','),
    turnoverList: data.data.turnoverList.split(','),
  }
}

const getUserStatisticsData = async (begin: string, end: string) => {
  const { data: res } = await getUserStatisticsAPI({ begin, end })
  userData.value = {
    dateList: res.data.dateList.split(','),
    totalUserList: res.data.totalUserList.split(','),
    newUserList: res.data.newUserList.split(','),
  }
}

const getOrderStatisticsData = async (begin: string, end: string) => {
  const { data: res } = await getOrderStatisticsAPI({ begin, end })
  orderData.value = {
    data: {
      dateList: res.data.dateList.split(','),
      orderCountList: res.data.orderCountList.split(','),
      validOrderCountList: res.data.validOrderCountList.split(','),
    },
    totalOrderCount: res.data.totalOrderCount,
    validOrderCount: res.data.validOrderCount,
    orderCompletionRate: res.data.orderCompletionRate,
  }
}

const getTopData = async (begin: string, end: string) => {
  const { data: res } = await getTop10StatisticsAPI({ begin, end })
  top10Data.value = {
    nameList: res.data.nameList.split(',').reverse(),
    numberList: res.data.numberList.split(',').reverse(),
  }
}

const getTitleNum = (data: number) => {
  switch (data) {
    case 1:
      tateData.value = get1stAndToday()
      break
    case 2:
      tateData.value = past7Day()
      break
    case 3:
      tateData.value = past30Day()
      break
    case 4:
      tateData.value = pastWeek()
      break
    case 5:
      tateData.value = pastMonth()
      break
  }
  init(tateData.value[0], tateData.value[1])
}

watch(nowIndex, (val) => {
  getTitleNum(val + 1)
})

const toggleTabs = (index: number) => {
  nowIndex.value = index
}

const handleExport = async () => {
  try {
    const confirm = await ElMessageBox.confirm('是否导出最近 30 天的运营数据？', '导出数据', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })

    if (confirm) {
      const { data } = await exportInforAPI()
      const url = window.URL.createObjectURL(data)
      const a = document.createElement('a')
      document.body.appendChild(a)
      a.href = url
      a.download = '运营数据统计报表.xlsx'
      a.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    }
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消导出')
      return
    }
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  getTitleNum(2)
})
</script>

<template>
  <div class="statistics-shell">
    <div class="page-head">
      <div class="page-head__meta">
        <h2>经营数据统计</h2>
        <p>围绕营收、用户、订单与销量 Top10 提供同一视觉体系下的数据分析页面。</p>
      </div>
      <div class="page-head__stats">
        <span class="page-stat">总订单 <strong>{{ orderData.totalOrderCount || 0 }}</strong></span>
        <span class="page-stat">有效订单 <strong>{{ orderData.validOrderCount || 0 }}</strong></span>
        <span class="page-stat">完成率 <strong>{{ ((orderData.orderCompletionRate || 0) * 100).toFixed(0) }}%</strong></span>
      </div>
    </div>

    <div class="statistics-toolbar">
      <div class="tab-change">
        <div
          v-for="(item, index) in tabsParam"
          :key="index"
          class="tab-item"
          :class="{ active: index === nowIndex }"
          @click="toggleTabs(index)"
        >
          <div class="item">{{ item }}</div>
        </div>
      </div>
      <div class="time-range">已选时间：{{ tateData[0] }} 至 {{ tateData[tateData.length - 1] }}</div>
      <el-button type="primary" size="large" @click="handleExport">导出数据报表</el-button>
    </div>

    <div class="statistics-grid">
      <section class="stat-card">
        <TurnoverStatistics :turnoverdata="turnoverData" />
      </section>
      <section class="stat-card">
        <UserStatistics :userdata="userData" />
      </section>
      <section class="stat-card">
        <OrderStatistics :orderdata="orderData" :overviewData="overviewData" />
      </section>
      <section class="stat-card">
        <Top :top10data="top10Data" />
      </section>
    </div>
  </div>
</template>

<style lang="less" scoped>
.statistics-shell {
  padding: 10px 6px 24px;
}

.statistics-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tab-change {
  display: inline-flex;
  border-radius: 999px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(23, 50, 57, 0.06);
}

.tab-item {
  min-width: 98px;
  padding: 10px 14px;
  text-align: center;
  border-radius: 999px;
  color: var(--text-sub);
  cursor: pointer;
  transition: 0.2s ease;
}

.tab-item.active {
  background: linear-gradient(135deg, var(--brand), var(--brand-deep));
  color: #fff;
  box-shadow: 0 12px 22px rgba(239, 143, 53, 0.24);
}

.time-range {
  padding: 0.8rem 1rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--text-sub);
  box-shadow: inset 0 0 0 1px rgba(23, 50, 57, 0.06);
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.stat-card {
  min-height: 420px;
  padding: 22px;
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-md);
}

@media (max-width: 1024px) {
  .statistics-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .statistics-toolbar {
    align-items: stretch;
  }

  .tab-change {
    flex-wrap: wrap;
  }

  .time-range {
    width: 100%;
  }
}
</style>
