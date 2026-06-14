<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCategoryAPI, getCategoryPageListAPI, updateCategoryStatusAPI } from '@/api/category'

interface Category {
  id: number
  name: string
  type: number
  sort: string
  status: number
  updateTime: string
}

const router = useRouter()
const categoryList = ref<Category[]>([])
const pageData = reactive({
  name: '',
  type: '',
  page: 1,
  pageSize: 6,
  total: 0,
})

const options = [
  { value: '1', label: '菜品分类' },
  { value: '2', label: '套餐分类' },
]

const init = async () => {
  const { data: res } = await getCategoryPageListAPI({
    name: pageData.name,
    type: pageData.type,
    page: pageData.page,
    pageSize: pageData.pageSize,
  })
  categoryList.value = res.data.records
  pageData.total = res.data.total
}

init()

const handleCurrentChange = (val: number) => {
  pageData.page = val
  init()
}

const handleSizeChange = (val: number) => {
  pageData.pageSize = val
  init()
}

const updateBtn = (row: Category) => {
  router.push({
    name: 'category_update',
    query: { id: row.id },
  })
}

const changeBtn = async (row: Category) => {
  await updateCategoryStatusAPI(row.id)
  await init()
  ElMessage.success('状态修改成功')
}

const deleteBtn = (row: Category) => {
  ElMessageBox.confirm('该操作会永久删除该分类，是否继续？', '删除分类', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await deleteCategoryAPI(row.id)
      await init()
      ElMessage.success('删除成功')
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}
</script>

<template>
  <el-card class="page-card">
    <div class="page-head">
      <div class="page-head__meta">
        <h2>分类管理</h2>
        <p>集中管理菜品分类与套餐分类，让商品结构更清楚、页面更整齐。</p>
      </div>
      <div class="page-head__stats">
        <span class="page-stat">分类总数 <strong>{{ pageData.total }}</strong></span>
        <span class="page-stat">每页展示 <strong>{{ pageData.pageSize }}</strong></span>
      </div>
    </div>

    <div class="page-toolbar">
      <el-input v-model="pageData.name" size="large" placeholder="输入分类名称搜索" clearable />
      <el-select v-model="pageData.type" size="large" clearable placeholder="选择分类类型">
        <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <div class="toolbar-spacer"></div>
      <el-button size="large" type="success" @click="init()">查询分类</el-button>
      <el-button size="large" type="primary" @click="router.push('/category/add')">
        <el-icon style="font-size: 15px; margin-right: 10px;">
          <Plus />
        </el-icon>
        添加分类
      </el-button>
    </div>

    <el-table :data="categoryList" stripe>
      <el-table-column prop="name" label="分类名称" align="center" />
      <el-table-column prop="type" label="类别" align="center">
        <template #default="scope">
          <span>{{ scope.row.type === 1 ? '菜品分类' : '套餐分类' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" align="center" />
      <el-table-column prop="status" label="状态" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" round>
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最近操作时间" width="220" align="center" />
      <el-table-column label="操作" width="240" align="center">
        <template #default="scope">
          <el-button type="primary" @click="updateBtn(scope.row)">修改</el-button>
          <el-button plain :type="scope.row.status === 1 ? 'danger' : 'primary'" @click="changeBtn(scope.row)">
            {{ scope.row.status === 1 ? '停售' : '起售' }}
          </el-button>
          <el-button type="danger" @click="deleteBtn(scope.row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无分类数据" />
      </template>
    </el-table>

    <el-pagination
      class="page"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pageData.total"
      :page-sizes="[2, 4, 6, 8]"
      v-model:current-page="pageData.page"
      v-model:page-size="pageData.pageSize"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </el-card>
</template>

<style lang="less" scoped>
.page-card {
  :deep(.el-card__body) {
    padding: 28px;
  }
}

:deep(.el-table tr) {
  font-size: 13px;
}

:deep(.el-table .el-button) {
  min-width: 56px;
}

.el-pagination {
  justify-content: center;
}
</style>
