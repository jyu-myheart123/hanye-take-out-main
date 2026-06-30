<template>
  <div class="container">
    <h2 class="homeTitle">
      套餐总览
      <div class="more">
        <router-link to="setmeal">套餐管理</router-link>
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
          <span class="num">{{ setMealData.sold }}</span>
        </li>
        <li>
          <div class="stat-item">
            <el-icon class="stat-icon"><Lock /></el-icon>
            <span class="stat-label">已停售</span>
          </div>
          <span class="num">{{ setMealData.discontinued }}</span>
        </li>
        <li class="add">
          <router-link to="setmeal/add">
            <el-icon class="add-icon"><CirclePlus /></el-icon>
            <span class="add-label">新增套餐</span>
          </router-link>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
interface SetMealData {
  sold: number;
  discontinued: number;
}

const props = defineProps<{
  setMealData: SetMealData;
}>()

</script>
