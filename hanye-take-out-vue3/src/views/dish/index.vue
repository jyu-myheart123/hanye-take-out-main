<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElTable } from 'element-plus'
import { deleteDishesAPI, getDishPageListAPI, updateDishStatusAPI } from '@/api/dish'
import { getCategoryPageListAPI } from '@/api/category'

interface Dish {
  id: number
  name: string
  pic: string
  detail: string
  price: number
  status: number
  categoryId: number
  updateTime: string
}

interface Category {
  id: number
  name: string
}

const router = useRouter()
const dishList = ref<Dish[]>([])
const categoryList = ref<Category[]>([])
const total = ref(0)
const multiTableRef = ref<InstanceType<typeof ElTable>>()
const multiSelection = ref<Dish[]>([])

const pageData = reactive({
  name: '',
  categoryId: '',
  status: '',
  page: 1,
  pageSize: 6,
})

const options = [
  { value: '1', label: '起售' },
  { value: '0', label: '停售' },
]

const init = async () => {
  const { data: resCategory } = await getCategoryPageListAPI({ page: 1, pageSize: 100, type: 1 })
  categoryList.value = resCategory.data.records
}

const showPageList = async () => {
  const { data: res } = await getDishPageListAPI(pageData)
  dishList.value = res.data.records
  total.value = res.data.total
}

init()
showPageList()

const handleCurrentChange = (val: number) => {
  pageData.page = val
  showPageList()
}

const handleSizeChange = (val: number) => {
  pageData.pageSize = val
  showPageList()
}

const handleSelectionChange = (val: Dish[]) => {
  multiSelection.value = val
}

const toAddUpdate = (row?: Dish) => {
  if (row?.id) {
    router.push({
      path: '/dish/add',
      query: { id: row.id },
    })
    return
  }
  router.push('/dish/add')
}

const changeBtn = async (row: Dish) => {
  await updateDishStatusAPI(row.id)
  await showPageList()
  ElMessage.success('状态修改成功')
}

const deleteBatch = (row?: Dish) => {
  ElMessageBox.confirm('该操作会永久删除菜品，是否继续？', '删除菜品', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      if (!row) {
        if (multiSelection.value.length === 0) {
          ElMessage.warning('请先勾选需要删除的菜品')
          return
        }
        const ids = multiSelection.value.map((item) => item.id).join(',')
        const res = await deleteDishesAPI(ids)
        if (res.data.code !== 0) return
      } else {
        const res = await deleteDishesAPI(String(row.id))
        if (res.data.code !== 0) return
      }
      await showPageList()
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
        <h2>菜品管理</h2>
        <p>更聚焦地查看菜品图片、分类、售价和上下架状态，支持批量删除。</p>
      </div>
      <div class="page-head__stats">
        <span class="page-stat">菜品总数 <strong>{{ total }}</strong></span>
        <span class="page-stat">已选条目 <strong>{{ multiSelection.length }}</strong></span>
      </div>
    </div>

    <div class="page-toolbar">
      <el-input v-model="pageData.name" size="large" placeholder="输入菜品名称搜索" clearable />
      <el-select v-model="pageData.categoryId" size="large" clearable placeholder="选择分类">
        <el-option v-for="item in categoryList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="pageData.status" size="large" clearable placeholder="选择售卖状态">
        <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <div class="toolbar-spacer"></div>
      <el-button size="large" type="success" @click="showPageList()">查询菜品</el-button>
      <el-button size="large" type="danger" plain @click="deleteBatch()">批量删除</el-button>
      <el-button size="large" type="primary" @click="toAddUpdate()">
        <el-icon style="font-size: 15px; margin-right: 10px;">
          <Plus />
        </el-icon>
        添加菜品
      </el-button>
    </div>

    <el-table ref="multiTableRef" :data="dishList" stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="菜名" align="center" />
      <el-table-column prop="pic" label="图片" align="center">
        <template #default="scope">
          <img v-if="scope.row.pic" :src="scope.row.pic" alt="" class="page-table-image" />
          <img v-else src="/src/assets/image/user_default.png" alt="" class="page-table-image" />
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" width="220" align="center" show-overflow-tooltip />
      <el-table-column prop="price" label="价格" align="center" />
      <el-table-column prop="status" label="状态" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" round>
            {{ scope.row.status === 1 ? '起售' : '停售' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="categoryId" label="所属分类" align="center">
        <template #default="scope">
          {{ categoryList.find((item) => item.id === scope.row.categoryId)?.name }}
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最近操作时间" width="180" align="center" />
      <el-table-column label="操作" width="220" align="center">
        <template #default="scope">
          <el-button type="primary" @click="toAddUpdate(scope.row)">修改</el-button>
          <el-button plain :type="scope.row.status === 1 ? 'danger' : 'primary'" @click="changeBtn(scope.row)">
            {{ scope.row.status === 1 ? '停售' : '起售' }}
          </el-button>
          <el-button type="danger" @click="deleteBatch(scope.row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无菜品数据" />
      </template>
    </el-table>

    <el-pagination
      class="page"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
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
