<template>
  <div class="container">
    <h2 class="homeTitle">
      菜品总览
      <div class="more">
        <router-link to="dish">菜品管理</router-link>
        <el-icon>
          <ArrowRight />
        </el-icon>
      </div>
    </h2>
    <div class="orderviewBox">
      <ul>
        <li>
          <div class="stat-item">
            <el-icon class="stat-icon"><Finished /></el-icon>
            <span class="stat-label">已启售</span>
          </div>
          <span class="num">{{ dishesData.sold }}</span>
        </li>
        <li>
          <div class="stat-item">
            <el-icon class="stat-icon"><Lock /></el-icon>
            <span class="stat-label">已停售</span>
          </div>
          <span class="num">{{ dishesData.discontinued }}</span>
        </li>
        <li class="add">
          <router-link to="/dish/add">
            <el-icon class="add-icon"><CirclePlus /></el-icon>
            <span class="add-label">新增菜品</span>
          </router-link>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>

// Define props
const props = defineProps<{
  dishesData: {
    sold: number,
    discontinued: number
  }
}>()
</script>

<style scoped></style>
