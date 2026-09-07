<template>
  <div class="container">
    <h2 class="homeTitle">
      今日数据<i>{{ days[1] }}</i>
      <div class="more">
        <router-link to="statistics">详细数据</router-link>
        <el-icon>
          <ArrowRight />
        </el-icon>
      </div>
    </h2>
    <div class="overviewBox">
      <ul>
        <li>
          <span class="metric-icon metric-icon--money">
            <el-icon><Money /></el-icon>
          </span>
          <p class="tit">营业额</p>
          <p class="num num--money">¥ {{ formatMoney(overviewData.turnover) }}</p>
        </li>
        <li>
          <span class="metric-icon metric-icon--order">
            <el-icon><Tickets /></el-icon>
          </span>
          <p class="tit">有效订单</p>
          <p class="num">{{ overviewData.validOrderCount }}</p>
        </li>
        <li>
          <span class="metric-icon metric-icon--rate">
            <el-icon><CircleCheck /></el-icon>
          </span>
          <p class="tit">订单完成率</p>
          <p class="num">
            {{ (overviewData.orderCompletionRate * 100).toFixed(0) }}%
          </p>
        </li>
        <li>
          <span class="metric-icon metric-icon--price">
            <el-icon><ShoppingBag /></el-icon>
          </span>
          <p class="tit">平均客单价</p>
          <p class="num">¥ {{ formatMoney(overviewData.unitPrice) }}</p>
        </li>
        <li>
          <span class="metric-icon metric-icon--user">
            <el-icon><User /></el-icon>
          </span>
          <p class="tit">新增用户</p>
          <p class="num">{{ overviewData.newUsers }}</p>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { getday } from '@/utils/date'

// Define props
const props = defineProps<{
  overviewData: {
    turnover: number
    validOrderCount: number
    orderCompletionRate: number
    unitPrice: number
    newUsers: number
  }
}>()

// Computed property for days
const days = computed(() => getday())

/**
 * 把金额数字格式化成「千分位 + 两位小数」的样子
 * 小白讲解：比如 12580.5 会变成 "12,580.50"，读起来更像钱
 */
const formatMoney = (value: number) =>
  value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
</script>

<style lang="less" scoped>
</style>
