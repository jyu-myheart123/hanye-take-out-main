<template>
  <div class="dashboard-container">
    <overview :overview-data="overviewData" />
    <div class="dashboard-row">
      <dishStatistics :dishes-data="dishesData" />
      <setmealStatistics :set-meal-data="setMealData" />
    </div>
    <orderList />
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import overview from './components/overview.vue'
import dishStatistics from './components/dishStatistics.vue'
import setmealStatistics from './components/setmealStatistics.vue'
import orderList from './components/orderList.vue'

const overviewData = ref({
  turnover: 0,
  validOrderCount: 0,
  orderCompletionRate: 0,
  unitPrice: 0,
  newUsers: 0
})

const dishesData = ref({
  sold: 0,
  discontinued: 0
})

const setMealData = ref({
  sold: 0,
  discontinued: 0
})

const fetchOverviewData = async () => {
  try {
    overviewData.value = {
      turnover: 12580.50,
      validOrderCount: 156,
      orderCompletionRate: 0.98,
      unitPrice: 80.65,
      newUsers: 28
    }
  } catch (error) {
    console.error('获取概览数据失败:', error)
  }
}

const fetchDishesData = async () => {
  try {
    dishesData.value = {
      sold: 85,
      discontinued: 12
    }
  } catch (error) {
    console.error('获取菜品数据失败:', error)
  }
}

const fetchSetMealData = async () => {
  try {
    setMealData.value = {
      sold: 23,
      discontinued: 5
    }
  } catch (error) {
    console.error('获取套餐数据失败:', error)
  }
}

onMounted(() => {
  fetchOverviewData()
  fetchDishesData()
  fetchSetMealData()
})
</script>

<style lang="less" scoped>
.dashboard-container {
  padding: 20px;
}

.dashboard-row {
  display: flex;
  gap: 20px;
  margin-top: 20px;

  > div {
    flex: 1;
    min-width: 0;
  }
}
</style>

<!--
  控制台内容区的全局样式（注意：这里故意不加 scoped）
  小白讲解：子组件（overview/dishStatistics 等）内部的元素，scoped 样式管不到，
  所以用一个不带 scoped 的 style 块来写；同时所有选择器都加上 .dashboard-container
  前缀当「命名空间」，保证这些样式只在控制台页面生效，不会串到别的页面去。
-->
<style lang="less">
.dashboard-container {
  // 卡片容器：和侧边栏、登录页同一套「玻璃拟态」语言（半透明 + 模糊 + 大圆角 + 柔影）
  .container {
    padding: 24px 26px;
    background: var(--surface);
    backdrop-filter: blur(18px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-md);
  }

  // 卡片标题行：标题在左，「详细数据 >」链接在右
  .homeTitle {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 0 20px;
    font-size: 1.2rem;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text-main);

    // 标题旁的日期小胶囊
    i {
      font-style: normal;
      padding: 4px 12px;
      border-radius: 999px;
      background: var(--brand-soft);
      color: var(--brand-deep);
      font-size: 0.82rem;
      font-weight: 600;
    }

    // 右侧「详细数据 >」链接，做成小胶囊按钮
    .more {
      margin-left: auto;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 7px 14px;
      border-radius: 999px;
      background: var(--brand-soft);
      color: var(--brand-deep);
      font-size: 0.85rem;
      font-weight: 600;
      transition: background 0.2s ease, color 0.2s ease;

      .el-icon {
        font-size: 0.9rem;
      }
    }

    .more:hover {
      background: var(--brand);
      color: #fff;
    }
  }

  // ===== 今日数据：5 个指标卡片，横排网格 =====
  .overviewBox ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 14px;

    li {
      padding: 18px 16px;
      background: rgba(255, 255, 255, 0.72);
      border: 1px solid var(--line-soft);
      border-radius: var(--radius-lg);
      transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

      &:hover {
        transform: translateY(-3px);
        box-shadow: var(--shadow-md);
        border-color: rgba(239, 143, 53, 0.35);
      }
    }

    // 指标图标：圆角小方块，五种语义配色
    .metric-icon {
      width: 38px;
      height: 38px;
      margin-bottom: 12px;
      border-radius: 12px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 1.15rem;
    }

    .metric-icon--money {
      background: var(--brand-soft);
      color: var(--brand-deep);
    }

    .metric-icon--order {
      background: var(--accent-soft);
      color: var(--accent);
    }

    .metric-icon--rate {
      background: rgba(32, 144, 113, 0.14);
      color: var(--el-color-success);
    }

    .metric-icon--price {
      background: rgba(219, 139, 45, 0.14);
      color: var(--el-color-warning);
    }

    .metric-icon--user {
      background: rgba(91, 110, 225, 0.14);
      color: #5b6ee1;
    }

    .tit {
      margin: 0 0 6px;
      font-size: 0.85rem;
      color: var(--text-sub);
    }

    // 大数字：加粗、等宽数字（tabular-nums 让数字对齐不跳动）
    .num {
      margin: 0;
      font-size: 1.55rem;
      font-weight: 800;
      line-height: 1.2;
      letter-spacing: -0.02em;
      color: var(--text-main);
      font-variant-numeric: tabular-nums;
    }

    // 营业额作为核心指标，用品牌橙强调
    .num--money {
      color: var(--brand-deep);
    }
  }

  // ===== 菜品 / 套餐总览：3 列网格 =====
  .orderviewBox ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;

    li {
      padding: 18px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      background: rgba(255, 255, 255, 0.72);
      border: 1px solid var(--line-soft);
      border-radius: var(--radius-lg);
    }

    .stat-item {
      display: flex;
      align-items: center;
      gap: 10px;

      .stat-icon {
        width: 36px;
        height: 36px;
        border-radius: 12px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 1.1rem;
      }

      .stat-label {
        color: var(--text-sub);
        font-size: 0.92rem;
      }
    }

    .num {
      font-size: 1.7rem;
      font-weight: 800;
      font-variant-numeric: tabular-nums;
    }

    // 第 1 项「已启售」：绿色语义
    li:nth-child(1) {
      .stat-icon {
        background: rgba(32, 144, 113, 0.14);
        color: var(--el-color-success);
      }

      .num {
        color: var(--el-color-success);
      }
    }

    // 第 2 项「已停售」：灰色语义
    li:nth-child(2) {
      .stat-icon {
        background: rgba(111, 125, 130, 0.14);
        color: var(--el-color-info);
      }

      .num {
        color: var(--text-sub);
      }
    }

    // 第 3 项「新增菜品 / 套餐」：虚线边框的添加按钮
    li.add {
      background: transparent;
      border: 1.5px dashed rgba(239, 143, 53, 0.45);
      justify-content: center;
      transition: background 0.2s ease, border-color 0.2s ease;

      a {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: var(--brand-deep);
        font-size: 0.95rem;
        font-weight: 600;
      }

      .add-icon {
        font-size: 1.2rem;
      }
    }

    li.add:hover {
      background: var(--brand-soft);
      border-color: var(--brand);
    }
  }

  // ===== 订单信息卡片 =====
  .homecon {
    margin-top: 20px;
  }

  // 订单状态切换 tab（待接单 / 待派送）：胶囊按钮组
  .homeTitleBtn {
    .conTab {
      margin: 0 0 0 auto;
      padding: 0;
      list-style: none;
      display: flex;
      gap: 10px;

      li {
        padding: 8px 18px;
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

        // 选中态：和侧边栏菜单选中项同款橙色渐变
        &.active {
          background: linear-gradient(135deg, rgba(239, 143, 53, 0.96), rgba(221, 107, 32, 0.92));
          border-color: transparent;
          color: #fff;
          box-shadow: 0 10px 22px rgba(239, 143, 53, 0.28);
        }
      }

      // 选中态下，角标数字改成白底橙字，避免红配橙太花
      li.active .el-badge__content {
        background: #fff;
        color: var(--brand-deep);
        border: none;
      }
    }
  }

  // 订单表格放进卡片后，收一下圆角和边框，和卡片融为一体
  .tableBox {
    border: 1px solid var(--line-soft);
    border-radius: 16px;
    overflow: hidden;
  }
}

// ===== 响应式：屏幕变窄时网格自动折行 =====
@media (max-width: 1280px) {
  .dashboard-container .overviewBox ul {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    .overviewBox ul {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .orderviewBox ul {
      grid-template-columns: 1fr;
    }

    .dashboard-row {
      flex-direction: column;
    }
  }
}
</style>