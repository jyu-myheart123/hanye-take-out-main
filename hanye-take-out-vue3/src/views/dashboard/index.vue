<template>
  <div class="dashboard-container home">
    <div class="page-head dashboard-head">
      <div class="page-head__meta">
        <h2>门店经营看板</h2>
        <p>把今日营收、订单处理进度、商品概览和实时待办压缩到同一张驾驶舱页面。</p>
      </div>
      <div class="page-head__stats">
        <span class="page-stat">今日营业额 <strong>￥{{ overviewData.turnover || 0 }}</strong></span>
        <span class="page-stat">待接订单 <strong>{{ orderStatics.toBeConfirmed || 0 }}</strong></span>
        <span class="page-stat">配送中 <strong>{{ orderStatics.deliveryInProgress || 0 }}</strong></span>
      </div>
    </div>

    <Overview :overviewData="overviewData" />
    <Orderview :orderviewData="orderviewData" />

    <div class="homeMain">
      <CuisineStatistics :dishesData="dishesData" />
      <SetMealStatistics :setMealData="setMealData" />
    </div>

    <OrderList :order-statics="orderStatics" @getOrderListBy3Status="getOrderListBy3Status" />
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { getBusinessDataAPI, getOrderDataAPI, getOverviewDishesAPI, getSetMealStatisticsAPI } from '@/api/dashboard'
import { getOrderListByAPI } from '@/api/order'
import Overview from './components/overview.vue'
import Orderview from './components/orderview.vue'
import CuisineStatistics from './components/dishStatistics.vue'
import SetMealStatistics from './components/setmealStatistics.vue'
import OrderList from './components/orderList.vue'

const overviewData = ref<any>({})
const orderviewData = ref<any>({})
const dishesData = ref<any>({})
const setMealData = ref<any>({})
const orderStatics = ref<any>({})

const init = async () => {
  try {
    const businessData = await getBusinessDataAPI()
    overviewData.value = businessData.data.data

    const orderData = await getOrderDataAPI()
    orderviewData.value = orderData.data.data

    const overviewDishes = await getOverviewDishesAPI()
    dishesData.value = overviewDishes.data.data

    const setMealStatistics = await getSetMealStatisticsAPI()
    setMealData.value = setMealStatistics.data.data

    await getOrderListBy3Status()
  } catch (error) {
    console.error('初始化首页数据失败: ', error)
  }
}

const getOrderListBy3Status = async () => {
  try {
    const res = await getOrderListByAPI()
    if (res.data.code === 0) {
      orderStatics.value = res.data.data
    } else {
      console.error(res.data.msg)
    }
  } catch (err) {
    console.error('请求订单状态统计失败: ', err)
  }
}

onMounted(() => {
  init()
})
</script>

<style lang="less">
li {
  list-style: none;
}

.dashboard-container {
  padding: 10px 6px 20px;
}

.dashboard-head {
  margin-bottom: 20px;
}

.homeTitle {
  font-weight: 700;
  font-size: 18px;
  color: var(--text-main);
  letter-spacing: -0.02em;
  padding-top: 4px;
  margin-bottom: 18px;

  i {
    display: inline-block;
    margin-left: 10px;
    padding: 4px 10px;
    border-radius: 999px;
    background: var(--brand-soft);
    color: var(--brand-deep);
    font-size: 12px;
    font-style: normal;
    font-weight: 700;
  }

  .more {
    display: flex;
    align-items: center;
    gap: 6px;
    float: right;
    color: var(--text-sub);
    font-size: 14px;
    font-weight: 500;

    a {
      display: inline-block;
      color: inherit;
    }

    .el-icon {
      font-size: 18px;
    }
  }
}

.homeMain {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;

  .container {
    margin-bottom: 0;
  }
}

.dashboard-container .container {
  position: relative;
  z-index: 1;
  background: rgba(255, 252, 247, 0.86);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.72);
  padding: 24px;
  border-radius: 28px;
  box-shadow: var(--shadow-md);
  margin-bottom: 20px;
}

.overviewBox ul,
.orderviewBox ul {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.overviewBox li,
.orderviewBox li {
  padding: 20px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(250, 244, 236, 0.96));
  border: 1px solid rgba(23, 50, 57, 0.06);
}

.overviewBox .tit,
.orderviewBox .status {
  color: var(--text-sub);
}

.overviewBox .num,
.orderviewBox .num {
  font-size: clamp(1.5rem, 2vw, 2rem);
  font-weight: 800;
  color: var(--text-main);
}

.overviewBox .num {
  padding-top: 12px;
}

.orderviewBox li {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 132px;
}

.orderviewBox .status {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  line-height: 1.4;

  .el-icon {
    font-size: 22px;
    color: var(--brand);
  }
}

.orderviewBox .num {
  margin-top: 18px;

  a {
    color: inherit;
  }
}

.conTab {
  float: right;
  font-weight: 500;
  font-size: 14px;
  color: var(--text-main);
  display: flex;
  height: 40px;
  line-height: 38px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(23, 50, 57, 0.08);
  border-radius: 999px;
  width: 250px;
  overflow: hidden;

  li {
    flex: 1;
    text-align: center;
    cursor: pointer;
    transition: 0.2s ease;

    &.active {
      background: linear-gradient(135deg, var(--brand), var(--brand-deep));
      color: #fff;
    }
  }

  .el-badge__content.is-fixed {
    top: 14px;
    right: 2px;
  }
}

@media (max-width: 1200px) {
  .overviewBox ul,
  .orderviewBox ul {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .homeMain {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overviewBox ul,
  .orderviewBox ul {
    grid-template-columns: 1fr;
  }
}
</style>
